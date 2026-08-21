package com.picker.BlinkitPicker.Services;

import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.picker.BlinkitPicker.Dto.request.SendOtpRequest;
import com.picker.BlinkitPicker.Dto.request.LoginRequest;
import com.picker.BlinkitPicker.Dto.request.RefreshTokenRequest;
import com.picker.BlinkitPicker.Dto.request.SignupRequest;
import com.picker.BlinkitPicker.Dto.request.VerifyOtpClientRequest;
import com.picker.BlinkitPicker.Dto.respons.LoginRespons;
import com.picker.BlinkitPicker.Dto.respons.SuccefullLoginResponse;
import com.picker.BlinkitPicker.Dto.respons.SuccessfullOtpResponse;
import com.picker.BlinkitPicker.Dto.respons.VerifyOtpRespons;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Model.UserHeaderModel;
import com.picker.BlinkitPicker.Repository.UserRepo;
import com.picker.BlinkitPicker.Util.ApiKeyGenerator;

import tools.jackson.databind.ObjectMapper;

@Service
public class AuthServices {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserRepo userRepo;
    private final JwtServices jwtServices;
    private final WebClientServices webClientServices;
    private final AdminServices adminServices;

    

    public AuthServices(UserRepo userRepo, JwtServices jwtServices, AdminServices adminServices, WebClientServices webClientServices) {
        this.userRepo = userRepo;
        this.jwtServices = jwtServices;
        this.adminServices = adminServices;
        this.webClientServices = webClientServices;
    }

    public ResponseEntity<?> sendOtp(SendOtpRequest request) {

        Optional<UserModel> userOpt = userRepo.findByPhone(request.getUserPhone());

         if (userOpt.isEmpty()) {

            SignupRequest signupRequest = SignupRequest.builder()
                    .phone(request.getUserPhone())
                    .plan("weekly")
                    .build();

           adminServices.addFreeUser(signupRequest);
           userOpt = userRepo.findByPhone(request.getUserPhone());
        }
    
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("user_phone", request.getUserPhone());
        formData.add("country_code",request.getCountryCode() != null ? request.getCountryCode() : "91");

        SuccessfullOtpResponse response = webClientServices.sendOtpToUser(formData, userOpt.get().getUserHeaders(),request);
        if (!response.isSuccess() && !response.isLogin()) {
            return ResponseEntity.status(500).body("Could not send OTP. Please try again later.");
        }

        return ResponseEntity.status(200).body(response);

    }

    public ResponseEntity<?> verifyOtp(VerifyOtpClientRequest request) {

        Optional<UserModel> userOpt = userRepo.findByPhone(request.getUserPhone());


        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("user_phone", request.getUserPhone());
        formData.add("verify_code", request.getVerifyCode());


        VerifyOtpRespons verifyRespons = webClientServices.verifyOtp(formData,userOpt.get().getUserHeaders(),request);

        if (!verifyRespons.getSuccess() && verifyRespons.getAccessToken() == null && !verifyRespons.getVerified()) {
            return ResponseEntity.status(500).body("Something went wrong while verifying OTP.");
        }


        String accessToken = verifyRespons.getAccessToken();
        String refreshToken = verifyRespons.getRefreshToken();

        
        if (userOpt.get().getApiKey()== null){
           userOpt.get().setApiKey(ApiKeyGenerator.generateApiKey());
        }
            
       

        SuccefullLoginResponse succesfullLoginResponse = webClientServices.login(LoginRequest.builder().phone(request.getUserPhone()).rfIdSupported(false).build(),
        userOpt.get().getUserHeaders(), accessToken);

        if(succesfullLoginResponse.getUserData().getPhone() != null && succesfullLoginResponse.getUserData().getRoleDetails().getUserId()!= null 
                && succesfullLoginResponse.getUserData().getRoleDetails().getEmployeeId()!= null) {
            UserHeaderModel header =  UserHeaderModel.builder()
                                  .xLat(request.getXLat().toString())
                                  .xLong(request.getXlong().toString())
                                  .employeeId(succesfullLoginResponse.getUserData().getRoleDetails().getEmployeeId())
                                  .employeeName(succesfullLoginResponse.getUserData().getName())
                                  .userId(succesfullLoginResponse.getUserData().getRoleDetails().getUserId())
                                  .role("PICKER")
                                  .siteId(succesfullLoginResponse.getUserData().getRoleDetails().getActiveSiteId())
                                  .siteName(succesfullLoginResponse.getUserData().getSiteName())
                                  .userHttpSessionToken(succesfullLoginResponse.getSessionToken())
                                  .userSessionToken(succesfullLoginResponse.getSessionToken())
                                  .accessToken(accessToken)
                                  .refreshToken(refreshToken)
                                  .build();

        userOpt.get().setUserHeaders(header);
        userRepo.save(userOpt.get());


      

        String applicationInternalToken = jwtServices.generateAccessToken(userOpt.get());
        String applicationInternalRefreshToken = jwtServices.generateRefreshToken(userOpt.get());

        return ResponseEntity.status(200).body(LoginRespons.builder()
                .token(applicationInternalToken)
                .refreshtoken(applicationInternalRefreshToken)
                .message("success")
                .build());
        }  else {
            return ResponseEntity.status(500).body(LoginRespons.builder()
                .token("")
                .refreshtoken("")
                .message("failed")
                .build());
        }
       
 
    }

    public ResponseEntity<?> refreshAccessToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (jwtServices.isTokenExpired(token)) {
            return ResponseEntity.status(401).body("Refresh token has expired");
        }

        Long userId;
        try {
            userId = jwtServices.extractUserId(token);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }

        Optional<UserModel> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }

        UserModel user = userOpt.get();
        String newAccessToken = jwtServices.generateAccessToken(user);
        String newRefreshToken = jwtServices.generateRefreshToken(user);

        return ResponseEntity.status(200).body(LoginRespons.builder()
                .token(newAccessToken)
                .refreshtoken(newRefreshToken)
                .message("success")
                .build());
    }

   /* 

     public Boolean generateInternalToken(String token) {

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Long userId = jwtServices.extractUserId(token);

        if (userId == null) {
            return false;
        }

        Optional<UserModel> user = userRepo.findById(userId);
        if (user.isEmpty()) {
            return false;
        }

        CognitoRefreshTokenRespons respons = webClientServices.refreshToken(user.get().getRefreshToken());
        if (respons.getAuthenticationResult() == null) {
            return null;
        }

        String freshToken = respons.getAuthenticationResult().getIdToken();
        user.get().setJwt(freshToken);
        if (user.get().getUserHeaders() != null) {
            user.get().getUserHeaders().setAuthorization(freshToken);
        }
        userRepo.save(user.get());

        return true;
    }

    private JsonNode extractJwtPayload(String token) throws Exception {
        String[] tokenParts = token.split("\\.");
        if (tokenParts.length < 2) {
            throw new IllegalArgumentException("Invalid JWT token");
        }

        byte[] decodedPayload = Base64.getUrlDecoder().decode(tokenParts[1]);
        return OBJECT_MAPPER.readTree(new String(decodedPayload, StandardCharsets.UTF_8));
    }

    private String getTextClaim(JsonNode claims, String claimName) {
        JsonNode claim = claims.get(claimName);
        if (claim == null || claim.isNull()) {
            return null;
        }
        return claim.asString();
    }

    private String extractPickerRole(String rolesJson) throws Exception {
        if (rolesJson == null || rolesJson.isBlank()) {
            return null;
        }

        JsonNode allowedRoles = OBJECT_MAPPER.readTree(rolesJson)
                .path("STOREOPS")
                .path("allowed_roles");

        if (allowedRoles.isArray()) {
            for (JsonNode allowedRole : allowedRoles) {
                if ("PICKER".equalsIgnoreCase(allowedRole.asString())) {
                    return "PICKER";
                }
            }
            return null;
        }

        String role = allowedRoles.asString();
        if (role == null) {
            return null;
        }

        for (String allowedRole : role.split(",")) {
            if ("PICKER".equalsIgnoreCase(allowedRole.trim())) {
                return "PICKER";
            }
        }

        return null;
    }
    
    */

   
}
