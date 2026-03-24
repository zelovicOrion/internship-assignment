package com.temenos.internship.assignment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "timer")
public class TimerProperties {
    private int maxRetries = 3;
    private int shortTimerThreshold = 3600;

}
