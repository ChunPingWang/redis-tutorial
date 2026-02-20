package com.tutorial.redis.module11;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.tutorial.redis.module11",
        "com.tutorial.redis.common"
})
public class Module11Application {
    public static void main(String[] args) {
        SpringApplication.run(Module11Application.class, args);
    }
}
