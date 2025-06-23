package com.ProjectApp.NonSenseGenerator.english;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

@Component //componente spring
public class GoogleTextModerationClient {

    private static final Path CREDENTIALS_PATH = Paths.get("src/main/resources/credentials.json"); //percorso dev
    private static final Path RUNTIME_PATH = Paths.get("target/classes/credentials.json"); //percorso runtime

    private String getApiKey() throws Exception {
        if (Files.exists(CREDENTIALS_PATH)) {
            return Files.readString(CREDENTIALS_PATH).trim(); //legge chiave in dev
        } else if (Files.exists(RUNTIME_PATH)) {
            return Files.readString(RUNTIME_PATH).trim(); //legge chiave in runtime
        } else {
            throw new RuntimeException("No API key found in credentials.json"); //errore se mancante
        }
    }

    public String moderateText(String text) {
        if (text.length() > 1000) text = text.substring(0, 1000); //limite API

        try {
            String apiKey = getApiKey(); //recupera la chiave

            JSONObject document = new JSONObject();
            document.put("type", "PLAIN_TEXT");
            document.put("content", text);

            JSONObject requestBody = new JSONObject();
            requestBody.put("document", document); //corpo della richiesta

            URL url = new URL("https://language.googleapis.com/v1/documents:moderateText?key=" + apiKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8)); //invio dati
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Moderation API error: HTTP " + responseCode);
                try (Scanner scanner = new Scanner(conn.getErrorStream()).useDelimiter("\\A")) {
                    System.err.println("Body: " + (scanner.hasNext() ? scanner.next() : ""));
                }
                return "API error"; //errore risposta
            }

            Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
            String response = scanner.hasNext() ? scanner.next() : ""; //legge risposta

            JSONObject json = new JSONObject(response);
            JSONObject result = json.optJSONObject("moderationResult");

            if (result != null) {
                StringBuilder output = new StringBuilder();
                for (String key : result.keySet()) {
                    double score = result.getDouble(key);
                    if (score > 0.5) {
                        output.append(key).append(": ").append(String.format("%.2f", score)).append(", "); //filtra sopra soglia
                    }
                }
                if (output.length() > 0) {
                    return output.substring(0, output.length() - 2); //rimuove virgola finale
                }
            }

            return "Clean"; //nessun contenuto tossico
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception occurred"; //errore generico
        }
    }
}
