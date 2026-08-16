package com.picker.BlinkitPicker.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import  com.picker.BlinkitPicker.Model.ApiModel;

public interface AppCaheRepo extends JpaRepository<ApiModel, String> {
    ApiModel findByApiName(String apiName);
    ApiModel findByApiUrl(String apiUrl);
    List<ApiModel> findAll();
}
