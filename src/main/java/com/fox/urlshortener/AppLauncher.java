package com.fox.urlshortener;

import java.util.TimeZone;

import com.fox.urlshortener.config.AppProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableScheduling
public class AppLauncher {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(AppLauncher.class, args);
    }
}
