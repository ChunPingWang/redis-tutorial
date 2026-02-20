package com.tutorial.redis.module10;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.tutorial.redis.module10",
        "com.tutorial.redis.common"
})
public class Module10Application {
    public static void main(String[] args) {
        SpringApplication.run(Module10Application.class, args);
    }
}
