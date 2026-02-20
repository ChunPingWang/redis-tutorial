package com.tutorial.redis.module13;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.tutorial.redis.module13",
        "com.tutorial.redis.common"
})
public class Module13Application {
    public static void main(String[] args) {
        SpringApplication.run(Module13Application.class, args);
    }
}
