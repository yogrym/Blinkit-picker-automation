package com.picker.BlinkitPicker.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health-check")
public class HealthCheck {

    @GetMapping("/v1/status")
    public String healthCheck() {
        return "OK - GitHub Actions Deployment Successful! 🚀";
    }
}
