package com.picker.BlinkitPicker.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.picker.BlinkitPicker.Model.UserModel;

@Repository
public interface UserRepo extends JpaRepository<UserModel, Long> {

    Optional<UserModel> findBytelegramUserName(String telegramUserName);

    Optional<UserModel> findByPhone(String phone);

    Optional<UserModel> findById(Long id);

    Optional<UserModel> findByApiKey(String apiKey);

}
