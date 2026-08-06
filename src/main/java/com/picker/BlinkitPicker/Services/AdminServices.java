package com.picker.BlinkitPicker.Services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.picker.BlinkitPicker.Dto.WorkerList;
import com.picker.BlinkitPicker.Dto.WorkerList.BookingData;
import com.picker.BlinkitPicker.Dto.request.SignupRequest;
import com.picker.BlinkitPicker.Dto.respons.AdminUserListResponse;
import com.picker.BlinkitPicker.Dto.respons.SignupRespons;

import java.util.concurrent.ConcurrentHashMap;
import com.picker.BlinkitPicker.Enums.RoleEnum;
import com.picker.BlinkitPicker.Model.UserHeaderModel;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Repository.UserRepo;
import com.picker.BlinkitPicker.Util.ApiKeyGenerator;

@Service
public class AdminServices {
    private static final int DEFAULT_USER_PAGE_SIZE = 25;
    private static final int MAX_USER_PAGE_SIZE = 100;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private JwtServices jwtServices;

    @Autowired
    private BookingServices bookingServices;

    public SignupRespons addUser(SignupRequest request) {
        Optional<UserModel> userOpt = userRepo.findByPhone(request.getPhone());

        if (userOpt.isPresent()) {
            return SignupRespons.builder()
                    .status("failure")
                    .message("cannot create new user, user exists with this phone number")
                    .build();
        }

        if (request.getRole() == null) {

            request.setRole(RoleEnum.USER);
        }

        LocalDateTime expiresAt = null;
        if (request.getPlan() != null) {
            String plan = request.getPlan().trim().toLowerCase();
            LocalDateTime now = LocalDateTime.now();
            if (plan.equals("weekly")) {
                expiresAt = now.plusWeeks(1);
            } else if (plan.equals("monthly")) {
                expiresAt = now.plusMonths(1);
            } else if (plan.equals("3 months") || plan.equals("3months")) {
                expiresAt = now.plusMonths(3);
            }
        }

        UserModel user = UserModel.builder()
                .phone(request.getPhone())
                .role(request.getRole())
                .apiKey(ApiKeyGenerator.generateApiKey())
                .expiresAt(expiresAt)
                .build();

        userRepo.save(user);

        return SignupRespons.builder()
                .status("success")
                .message("user created successfully")
                .build();

    }




     public SignupRespons addFreeUser(SignupRequest request) {
        Optional<UserModel> userOpt = userRepo.findByPhone(request.getPhone());

        if (userOpt.isPresent()) {
            return SignupRespons.builder()
                    .status("failure")
                    .message("cannot create new user, user exists with this phone number")
                    .build();
        }

        

        request.setRole(request.getRole() != null ? request.getRole() : RoleEnum.USER);

        LocalDateTime expiresAt = LocalDateTime.now().plusWeeks(1); 
       

        UserModel user = UserModel.builder()
                .phone(request.getPhone())
                .role(request.getRole())
                .apiKey(ApiKeyGenerator.generateApiKey())
                .expiresAt(expiresAt)
                .build();

        userRepo.save(user);

        return SignupRespons.builder()
                .status("success")
                .message("user created successfully")
                .build();

    }

    public AdminUserListResponse getUsers(Integer page, Integer size) {
        int safePage = page == null || page < 0 ? 0 : page;
        int requestedSize = size == null || size < 1 ? DEFAULT_USER_PAGE_SIZE : size;
        int safeSize = Math.min(requestedSize, MAX_USER_PAGE_SIZE);

        Page<UserModel> usersPage = userRepo.findAll(
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<AdminUserListResponse.UserData> users = usersPage.getContent()
                .stream()
                .map(this::toUserData)
                .toList();

        return AdminUserListResponse.builder()
                .users(users)
                .page(usersPage.getNumber())
                .size(usersPage.getSize())
                .totalPages(usersPage.getTotalPages())
                .totalUsers(usersPage.getTotalElements())
                .hasNext(usersPage.hasNext())
                .build();
    }

    public AdminUserListResponse searchUserByPhone(String phone) {
        List<AdminUserListResponse.UserData> users = userRepo.findByPhone(phone)
                .map(this::toUserData)
                .map(List::of)
                .orElseGet(List::of);

        return AdminUserListResponse.builder()
                .users(users)
                .page(0)
                .size(users.size())
                .totalPages(users.isEmpty() ? 0 : 1)
                .totalUsers(users.size())
                .hasNext(false)
                .build();
    }

    private AdminUserListResponse.UserData toUserData(UserModel user) {
        UserHeaderModel headers = user.getUserHeaders();

        List<BookingData> userBookingData = Collections.emptyList();
        if (bookingServices.getWorkerMap().containsKey(user.getId().toString())) {
            userBookingData = bookingServices.getWorkerMap().get(user.getId().toString()).getAllBookingData();
        }

        return AdminUserListResponse.UserData.builder()
                .id(user.getId())
                .employeeName(headers != null ? headers.getEmployeeName() : null)
                .employeeId(headers != null ? headers.getEmployeeId() : null)
                .phoneNumber(user.getPhone())
                .expired(user.getExpired())
                .blocked(user.getBlocked())
                .createdAt(user.getCreatedAt())
                .expiresAt(user.getExpiresAt())
                .role(user.getRole())
                .totalBookedSlots(user.getTotalBookedSlots() != null ? user.getTotalBookedSlots() : 0L)
                .userId(headers != null ? headers.getUserId() : null)
                .apiKey(user.getApiKey())
                .siteId(headers != null ? headers.getSiteId() : null)
                .bookingDataMap(userBookingData)
                .build();
    }

    public String deleteUser(Long userId) {
        if (!userRepo.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
        }
        userRepo.deleteById(userId);
        return "user deleted successfully";
    }

    public Boolean blockUser(Long userId) {
        UserModel user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        user.setBlocked(true);
        userRepo.save(user);
        return true;
    }

    public Boolean unblockUser(Long userId) {
        UserModel user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        user.setBlocked(false);
        userRepo.save(user);
        return true;
    }

    public String changeRole(Long userId, String roleStr) {
        UserModel user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        try {
            RoleEnum newRole = RoleEnum.valueOf(roleStr.toUpperCase());
            user.setRole(newRole);
            userRepo.save(user);
            return newRole.toString();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid role");
        }
    }

    public String changeStoreIdByAdminOrMaintainer(String token, Long targetUserId, String newStoreId) {
        var claims = jwtServices.extractClaimsSafely(token);
        if (claims == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
        }
        String currentUserRole = claims.get("role", String.class);

        UserModel targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "target user not found"));

        if ("MAINTAINER".equals(currentUserRole)) {
            if (targetUser.getRole() == RoleEnum.ADMIN || targetUser.getRole() == RoleEnum.MAINTAINER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "a maintainer cannot change the store id of an admin or another maintainer");
            }
        }

        UserHeaderModel headers = targetUser.getUserHeaders();
        if (headers == null) {
            headers = new UserHeaderModel();
        }
        headers.setSiteId(newStoreId);
        targetUser.setUserHeaders(headers);
        userRepo.save(targetUser);

        return "store id changed successfully";
    }

    public String stopUserSession(String token, Long targetUserId, String sessionId) {
        var claims = jwtServices.extractClaimsSafely(token);
        if (claims == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
        }
        String currentUserRole = claims.get("role", String.class);

        UserModel targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "target user not found"));

        if ("MAINTAINER".equals(currentUserRole)) {
            if (targetUser.getRole() == RoleEnum.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "a maintainer cannot stop an admin's sessions");
            }
        }

        ConcurrentHashMap<String, WorkerList> workerMap = bookingServices.getWorkerMap();
        if (workerMap.containsKey(targetUserId.toString())) {
            WorkerList userWorkers = workerMap.get(targetUserId.toString());
            BookingWorker worker = userWorkers.getWorker(sessionId);
            if (worker != null) {
                worker.stop();
                userWorkers.removeWorker(sessionId);
                return "Stopped " + sessionId;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "session not found");
    }

    public String pauseUserSession(String token, Long targetUserId, String sessionId) {
        var claims = jwtServices.extractClaimsSafely(token);
        if (claims == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
        }
        String currentUserRole = claims.get("role", String.class);

        UserModel targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "target user not found"));

        if ("MAINTAINER".equals(currentUserRole)) {
            if (targetUser.getRole() == RoleEnum.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "a maintainer cannot pause an admin's sessions");
            }
        }

        ConcurrentHashMap<String, WorkerList> workerMap = bookingServices.getWorkerMap();
        if (workerMap.containsKey(targetUserId.toString())) {
            WorkerList userWorkers = workerMap.get(targetUserId.toString());
            BookingWorker worker = userWorkers.getWorker(sessionId);
            if (worker != null) {
                if (worker.pause()) {
                    WorkerList.BookingData bookingData = userWorkers.getBookingDataMap().get(sessionId);
                    if (bookingData != null) {
                        bookingData.setIsPaused(true);
                    }
                }
                return "Paused " + sessionId;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "session not found");
    }

    public Boolean renewPlan(String token, Long userId, String planType) {

        var claims = jwtServices.extractClaimsSafely(token);
        if (claims == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
        }

        UserModel user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        LocalDateTime currentPlanValidity = user.getExpiresAt();

        if (planType != null) {

            if (planType.equals("weekly")) {
                user.setExpiresAt(currentPlanValidity.plusWeeks(1));
            } else if (planType.equals("monthly")) {
                user.setExpiresAt(currentPlanValidity.plusMonths(1));
            } else if (planType.equals("3 months") || planType.equals("3months")) {
                user.setExpiresAt(currentPlanValidity.plusMonths(3));
            }

        }

        userRepo.save(user);
        return true;

    }

}
