package com.eventdriven.integrationlayer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IntegrationLayerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationLayerApplication.class, args);
    }
}
