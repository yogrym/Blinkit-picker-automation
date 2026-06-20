package com.picker.BlinkitPicker.Services;

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

        UserModel user = UserModel.builder()
                .phone(request.getPhone())
                .apiKey(request.getApiKey())
                .role(request.getRole())
                .build();

        userRepo.save(user);

        return SignupRespons.builder()
                .status("success")
                .message("user created successfully")
                .userMobileNumber(user.getPhone())
                .loginKey(user.getApiKey())
                .build();

    }

}
