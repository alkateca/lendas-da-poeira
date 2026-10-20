package com.alkateca.lendasdapoeira;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LendasDaPoeiraApplication {

    public static void main(String[] args) {
        SpringApplication.run(LendasDaPoeiraApplication.class, args);
        System.out.println("==========================================================");
        System.out.println(">>> Lendas da Poeira - API Backend rodando na porta 8080! <<<");
        System.out.println(">>> Pronto para receber conexões do Front-End (Phaser.js) <<<");
        System.out.println("==========================================================");
    }
}