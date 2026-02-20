package com.tutorial.redis.module01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.tutorial.redis.module01",
        "com.tutorial.redis.common"
})
public class Module01Application {
    public static void main(String[] args) {
        SpringApplication.run(Module01Application.class, args);
    }
}
