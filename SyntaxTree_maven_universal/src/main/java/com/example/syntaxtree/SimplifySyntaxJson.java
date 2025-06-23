package com.example.syntaxtree;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

public class SimplifySyntaxJson {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java -cp json.jar:SimplifySyntaxJson.jar SimplifySyntaxJson input.json output.json");
            System.exit(1); //richiede input e output
        }

        try {
            String input = readFile(args[0]); //legge file input
            JSONObject json = new JSONObject(input);
            JSONArray tokens = json.getJSONArray("tokens");
            JSONArray simplified = new JSONArray();

            for (int i = 0; i < tokens.length(); i++) {
                JSONObject token = tokens.getJSONObject(i);
                String tag = token.getJSONObject("partOfSpeech").getString("tag");

                if ("PUNCT".equals(tag)) continue; //ignora punteggiatura

                JSONObject simpleToken = new JSONObject();
                simpleToken.put("content", token.getJSONObject("text").getString("content"));
                simpleToken.put("headTokenIndex", token.getJSONObject("dependencyEdge").getInt("headTokenIndex"));
                simpleToken.put("pos", tag);

                simplified.put(simpleToken); //aggiunge token semplificato
            }

            try (PrintWriter out = new PrintWriter(args[1])) {
                out.println(simplified.toString(2)); //scrive output
            }

            System.out.println("Simplified output written to " + args[1]);

        } catch (Exception e) {
            e.printStackTrace(); //gestione errore generica
        }
    }

    // legge contenuto file in una stringa
    private static String readFile(String filename) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            sb.append(line);
        reader.close();
        return sb.toString();
    }
}
