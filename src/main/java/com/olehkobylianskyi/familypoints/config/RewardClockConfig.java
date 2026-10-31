package com.olehkobylianskyi.familypoints.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class RewardClockConfig {
    @Bean public Clock rewardClock() { return Clock.systemUTC(); }
}
