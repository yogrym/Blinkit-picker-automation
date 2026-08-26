package com.picker.BlinkitPicker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BlinkitPickerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlinkitPickerApplication.class, args);
	}

}
