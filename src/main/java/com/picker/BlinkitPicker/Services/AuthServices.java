package com.picker.BlinkitPicker.Services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.picker.BlinkitPicker.Dto.LoginRequest;
import com.picker.BlinkitPicker.Dto.LoginRespons;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Repository.UserRepo;

@Service
public class AuthServices {

    private final UserRepo userRepo;
    private final JwtServices jwtServices;

    public AuthServices(UserRepo userRepo, JwtServices jwtServices) {
        this.userRepo = userRepo;
        this.jwtServices = jwtServices;
    }

    public LoginRespons login(LoginRequest loginRequest) {

        Optional<UserModel> userOpt = userRepo.findByApiKey(loginRequest.getKey());

        if (userOpt.isEmpty()) {
            return LoginRespons.builder()
                    .message("Invalid API Key. User not found.")
                    .build();
        }

        UserModel user = userOpt.get();

        if (user.getExpiresAt() != null &&
                user.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            return LoginRespons.builder()
                    .message("Your plan has expired. Please contact admin to renew your plan.")
                    .build();
        }

        String accessToken = jwtServices.generateAccessToken(user);
        String refreshToken = jwtServices.generateRefreshToken(user);

        user.setJwt(accessToken);
        userRepo.save(user);

        return LoginRespons.builder()
                .token(accessToken)
                .refreshtoken(refreshToken)
                .message("Login successful.")
                .build();
    }
}
