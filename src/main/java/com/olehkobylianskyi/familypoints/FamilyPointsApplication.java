package com.olehkobylianskyi.familypoints;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FamilyPointsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyPointsApplication.class, args);
    }

}
