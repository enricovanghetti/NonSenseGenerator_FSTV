package com.example.syntaxtree;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication //entry point spring boot
public class Application {
    public static void main(String[] args) {
        var context = SpringApplication.run(Application.class, args); //avvia app
        System.out.println("Porta attiva: " + context.getEnvironment().getProperty("server.port")); //stampa porta
    }
}
