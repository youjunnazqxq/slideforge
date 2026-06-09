package com.slideforge.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SlideForgeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SlideForgeApiApplication.class, args);
    }
}
