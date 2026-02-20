package com.tutorial.redis.module02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.tutorial.redis.module02",
        "com.tutorial.redis.common"
})
public class Module02Application {
    public static void main(String[] args) {
        SpringApplication.run(Module02Application.class, args);
    }
}
