package com.tutorial.redis.module07;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.tutorial.redis.module07",
        "com.tutorial.redis.common"
})
public class Module07Application {
    public static void main(String[] args) {
        SpringApplication.run(Module07Application.class, args);
    }
}
