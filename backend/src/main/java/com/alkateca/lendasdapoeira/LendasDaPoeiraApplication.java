package com.alkateca.lendasdapoeira;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LendasDaPoeiraApplication {
    public static void main(String[] args) {
        SpringApplication.run(LendasDaPoeiraApplication.class, args);
    }
}
