package com.picker.BlinkitPicker.Services;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.picker.BlinkitPicker.Dto.SignupRequest;
import com.picker.BlinkitPicker.Dto.SignupRespons;
import com.picker.BlinkitPicker.Enums.RoleEnum;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Repository.UserRepo;

@Service
public class AdminServices {
    @Autowired
    private UserRepo userRepo;

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
                .expiresAt(expiresAt)
                .build();

        userRepo.save(user);

        return SignupRespons.builder()
                .status("success")
                .message("user created successfully")
                .build();

    }

}
