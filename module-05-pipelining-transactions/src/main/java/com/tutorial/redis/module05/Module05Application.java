package com.tutorial.redis.module05;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.tutorial.redis.module05",
        "com.tutorial.redis.common"
})
public class Module05Application {
    public static void main(String[] args) {
        SpringApplication.run(Module05Application.class, args);
    }
}
