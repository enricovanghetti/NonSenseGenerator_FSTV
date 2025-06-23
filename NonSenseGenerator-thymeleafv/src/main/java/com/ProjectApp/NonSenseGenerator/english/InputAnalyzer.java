package com.ProjectApp.NonSenseGenerator.english;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component //gestito da spring
public class InputAnalyzer {

    public static FilteredTokens analyze(String text) throws Exception {
        List<String> nouns = new ArrayList<>(); //nomi trovati
        List<String> verbs = new ArrayList<>(); //verbi trovati
        List<String> adjectives = new ArrayList<>(); //aggettivi trovati

        Set<String> blacklist = Set.of(
            "yesterday", "today", "tomorrow", "anymore", "something", "nothing", "everything", //parole che venivano erroneamente ricnosciute come 'noun'
            "nigga", "niggas", "nigger", "bitch", "sex", "basatrd", "asshole", "jewish", "dick", "pines", "blowjob", "anal", "rape", "pussy", "hate", "sexual", "fuck", "fucking", "fucked"
        ); //parole vietate

        // legge api key da risorse
        InputStream in = InputAnalyzer.class.getClassLoader().getResourceAsStream("credentials.json");
        if (in == null) throw new FileNotFoundException("File credentials.json non trovato");
        String apiKey = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();

        // costruisce corpo JSON per richiesta
        JSONObject document = new JSONObject()
                .put("type", "PLAIN_TEXT")
                .put("content", text);

        JSONObject requestBody = new JSONObject()
                .put("document", document)
                .put("encodingType", "UTF16");

        // invia richiesta HTTP a Google NLP
        URL url = new URL("https://language.googleapis.com/v1/documents:analyzeSyntax?key=" + apiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8)); //invia json
        }

        // legge risposta
        String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(response);
        JSONArray tokens = json.getJSONArray("tokens");

        // filtra i token
        for (int i = 0; i < tokens.length(); i++) {
            JSONObject token = tokens.getJSONObject(i);
            String word = token.getJSONObject("text").getString("content").toLowerCase();
            if (blacklist.contains(word)) continue; //scarta vietate

            String tag = token.getJSONObject("partOfSpeech").getString("tag");
            switch (tag) {
                case "NOUN" -> nouns.add(word);
                case "VERB" -> verbs.add(word);
                case "ADJ" -> adjectives.add(word);
            }
        }

        // seleziona almeno due parole random
        List<String> selected = new ArrayList<>();
        Random rand = new Random();
        if (!nouns.isEmpty()) selected.add(nouns.get(rand.nextInt(nouns.size())));
        if (!verbs.isEmpty()) selected.add(verbs.get(rand.nextInt(verbs.size())));

        // aggiunge extra fino a 4 parole
        List<String> extra = new ArrayList<>();
        extra.addAll(nouns);
        extra.addAll(verbs);
        extra.addAll(adjectives);
        extra.removeAll(selected);
        Collections.shuffle(extra);

        while (selected.size() < 4 && !extra.isEmpty()) {
            selected.add(extra.remove(0));
        }

        // filtra i token selezionati
        Set<String> finalSet = new HashSet<>(selected);
        nouns.retainAll(finalSet);
        verbs.retainAll(finalSet);
        adjectives.retainAll(finalSet);

        return new FilteredTokens(nouns, verbs, adjectives); //ritorna solo i selezionati
    }

    public static String cleanGrammar(String s) {
        s = s
                .replaceAll("\\b(an) ([^aeiouAEIOU\\s])", "a $2") //corregge an/a
                .replaceAll("\\b(a) ([aeiouAEIOU])", "an $2") //corregge a/an
                .replaceAll("\\s+", " ") //spazi multipli
                .trim();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1); //iniziale maiuscola
    }
}
