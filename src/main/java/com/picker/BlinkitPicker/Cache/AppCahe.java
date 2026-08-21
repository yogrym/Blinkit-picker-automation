package com.picker.BlinkitPicker.Cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.picker.BlinkitPicker.Enums.ApiEnums;
import com.picker.BlinkitPicker.Model.ApiModel;
import com.picker.BlinkitPicker.Repository.AppCaheRepo;

import jakarta.annotation.PostConstruct;


@Component
public class AppCahe {

   private final AppCaheRepo appCaheRepo;
   private final Map<String, String> cache = new ConcurrentHashMap<>();

   public AppCahe(AppCaheRepo appCaheRepo) {
      this.appCaheRepo = appCaheRepo;
   }
   

   @PostConstruct
   public void init() {
      refresh();
   }

   public String getApiUrl(ApiEnums apiEnums) {
      return cache.get(apiEnums.name());
   }

   public boolean contains(ApiEnums apiEnums) {
      return cache.containsKey(apiEnums.name());
   }

   public void refresh() {
      cache.clear();
      for (ApiModel model : appCaheRepo.findAll()) {
         cache.put(model.getApiName(), model.getApiUrl());
      }
   }

   
}
