package com.tutorial.redis.module04;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.tutorial.redis.module04",
        "com.tutorial.redis.common"
})
public class Module04Application {
    public static void main(String[] args) {
        SpringApplication.run(Module04Application.class, args);
    }
}
