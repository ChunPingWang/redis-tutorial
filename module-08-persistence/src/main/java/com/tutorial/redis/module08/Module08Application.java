package com.tutorial.redis.module08;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.tutorial.redis.module08",
        "com.tutorial.redis.common"
})
public class Module08Application {
    public static void main(String[] args) {
        SpringApplication.run(Module08Application.class, args);
    }
}
