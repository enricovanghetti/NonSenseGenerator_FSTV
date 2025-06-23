package com.ProjectApp.NonSenseGenerator.util;

import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ApiKeyLoader {
    public static String loadApiKey(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path); //carica risorsa da classpath
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.readLine().trim(); //legge prima riga (la chiave)
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to load API key from " + path, e); //errore caricamento
        }
    }
}
