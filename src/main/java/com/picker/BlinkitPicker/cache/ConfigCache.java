package com.picker.BlinkitPicker.cache;

import java.util.HashMap;
import java.util.Map;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.picker.BlinkitPicker.Model.ApiModel;
import com.picker.BlinkitPicker.Repository.ApiConfigRepository;

import jakarta.annotation.PostConstruct;

@Component
public class ConfigCache {

    @Autowired
    private ApiConfigRepository apiConfigRepository;

    
    Map<String, String> appCache ;


    @PostConstruct
    public void init() {
       appCache = new HashMap<>();

       List<ApiModel> all = apiConfigRepository.findAll();
       for (ApiModel model : all) {
           appCache.put(model.getKey(), model.getValue());
       }
    }
}
