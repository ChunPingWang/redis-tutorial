package com.tutorial.redis.module06;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.tutorial.redis.module06",
        "com.tutorial.redis.common"
})
public class Module06Application {
    public static void main(String[] args) {
        SpringApplication.run(Module06Application.class, args);
    }
}
