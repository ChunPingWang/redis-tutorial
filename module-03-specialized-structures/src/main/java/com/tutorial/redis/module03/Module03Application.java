package com.tutorial.redis.module03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.tutorial.redis.module03",
        "com.tutorial.redis.common"
})
public class Module03Application {
    public static void main(String[] args) {
        SpringApplication.run(Module03Application.class, args);
    }
}
