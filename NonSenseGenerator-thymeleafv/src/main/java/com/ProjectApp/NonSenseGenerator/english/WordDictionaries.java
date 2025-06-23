package com.ProjectApp.NonSenseGenerator.english;

import org.springframework.stereotype.Component;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component //classe spring
public class WordDictionaries {

    private final List<String> nouns; //lista nomi
    private final List<String> adjectives; //lista aggettivi
    private final List<String> infinitiveVerbs; //verbi all'infinito
    private final List<String> gerundVerbs; //verbi in -ing
    private final List<String> verbs; //verbi generici
    private final List<String> articles = List.of("a", "an", "the"); //articoli inglesi

    public WordDictionaries() {
        nouns = loadWords("nouns.json");
        adjectives = loadWords("adjectives.json");
        infinitiveVerbs = loadWords("verbs-infinitive.json");
        gerundVerbs = loadWords("verbs-gerund.json");
        verbs = infinitiveVerbs; //stessa lista per semplicità
    }

    private List<String> loadWords(String filename) {
        try {
            InputStream inputStream = new ClassPathResource("dictionaries_en/" + filename).getInputStream(); //carica file
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)); //lettore file
            Type listType = new TypeToken<List<String>>() {}.getType(); //tipo generico
            return new Gson().fromJson(reader, listType); //parsing json
        } catch (Exception e) {
            System.err.println("Error loading " + filename + ": " + e.getMessage()); //errore lettura
            return Collections.emptyList(); //lista vuota fallback
        }
    }

    private String getRandomWord(List<String> list) {
        if (list == null || list.isEmpty()) return "???"; //fallback se lista vuota
        return list.get(new Random().nextInt(list.size())); //parola random
    }

    public String getRandomNoun() {
        return getRandomWord(nouns);
    }

    public String getRandomAdjective() {
        return getRandomWord(adjectives);
    }

    public String getRandomInfinitiveVerb() {
        return getRandomWord(infinitiveVerbs);
    }

    public String getRandomGerundVerb() {
        return getRandomWord(gerundVerbs);
    }

    public String getRandomVerb() {
        return getRandomWord(verbs);
    }

    public String getRandomArticle(String nextWord) {
        if (nextWord == null || nextWord.isEmpty()) return "the"; //fallback
        char firstChar = nextWord.toLowerCase().charAt(0); //prima lettera
        return "aeiou".indexOf(firstChar) >= 0 ? "an" : "a"; //sceglie articolo corretto
    }
}
