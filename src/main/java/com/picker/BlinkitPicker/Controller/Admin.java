package com.picker.BlinkitPicker.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class Admin {
    
    @GetMapping("/add-user")
    public String addUser(){
        return "Add user";
    }
}
