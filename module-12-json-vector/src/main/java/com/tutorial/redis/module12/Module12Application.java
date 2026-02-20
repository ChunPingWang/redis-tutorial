package com.tutorial.redis.module12;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.tutorial.redis.module12",
        "com.tutorial.redis.common"
})
public class Module12Application {
    public static void main(String[] args) {
        SpringApplication.run(Module12Application.class, args);
    }
}
