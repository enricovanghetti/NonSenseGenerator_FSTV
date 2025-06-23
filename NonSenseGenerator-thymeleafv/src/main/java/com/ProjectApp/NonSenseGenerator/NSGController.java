package com.ProjectApp.NonSenseGenerator;

import com.ProjectApp.NonSenseGenerator.english.FilteredTokens;
import com.ProjectApp.NonSenseGenerator.english.InputAnalyzer;
import com.ProjectApp.NonSenseGenerator.service.NonsenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;

@Controller //gestisce richieste web
public class NSGController {

    @Autowired
    private NonsenseService service; //logica per generare frasi

    @GetMapping("/") //homepage
    public String showForm(Model model) throws IOException {
        Path credentialsPath = Path.of("src/main/resources/credentials.json"); //path chiave api
        if (!Files.exists(credentialsPath) || Files.readString(credentialsPath).trim().isEmpty()) {
            return "redirect:/apikey"; //se non esiste reindirizza
        }
        model.addAttribute("isInitialLoad", true); //flag per animazione iniziale
        return "NSG"; //pagina principale
    }

    @GetMapping("/apikey") //pagina inserimento chiave api
    public String showApiKeyPage() {
        return "apikey";
    }

    @PostMapping("/validate-key") //validazione chiave api
    @ResponseBody
    public ResponseEntity<Void> validateApiKey(@RequestBody Map<String, String> body) {
        String apiKey = body.get("apiKey"); //estrai chiave

        String testJson = """
        {
          "document": {
            "type": "PLAIN_TEXT",
            "content": "Hello world"
          },
          "encodingType": "UTF8"
        }
        """;

        try {
            //invio richiesta test all'API
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://language.googleapis.com/v1/documents:analyzeSyntax?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(testJson))
                .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                //scrive la chiave api in due percorsi
                Path resourcePath = Path.of("src/main/resources/credentials.json"); //qui per far si che anche nelle sessioni future l'API key non venga piu' richeista
                Files.writeString(resourcePath, apiKey);

                Path runtimePath = Path.of("target/classes/credentials.json");//qui per renderla utilzzabile nella sessione attuale
                Files.createDirectories(runtimePath.getParent());
                Files.writeString(runtimePath, apiKey);

                return ResponseEntity.ok().build(); //ok se valida
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); //errore se non valida
    }

    @PostMapping("/generate") //genera frasi
    public String generate(@RequestParam("inputText") String inputText,
                           @RequestParam("sentenceCount") int sentenceCount,
                           Model model) {
        try {
            FilteredTokens tokens = InputAnalyzer.analyze(inputText); //analizza input
            String output = service.generateNonsenseFromTokens(tokens, sentenceCount); //genera frase

            model.addAttribute("nonsense", output); //output nel modello
            model.addAttribute("inputText", inputText);
            model.addAttribute("sentenceCount", sentenceCount);
            model.addAttribute("isInitialLoad", false);
            model.addAttribute("syntacticTree", false);

            return "NSG";
        } catch (Exception e) {
            model.addAttribute("nonsense", "Error: " + e.getMessage());
            model.addAttribute("isInitialLoad", false);
            return "NSG"; //pagina con errore
        }
    }

    @PostMapping("/generateTree") //genera albero sintattico
    @ResponseBody
    public String generateTree(@RequestBody Map<String, String> payload) {
        String inputText = payload.get("inputText");

        try {
            Path syntaxTreeDir = Path.of("../SyntaxTree_maven_universal").toAbsolutePath().normalize(); //path esterno
            Path treeImagePath = syntaxTreeDir.resolve("tree.png"); //output immagine
            Path inputJsonPath = syntaxTreeDir.resolve("input.json"); //input json per parser
            Path launcherPath = syntaxTreeDir.resolve("Launcher.java"); //entry point

            File treeImageFile = treeImagePath.toFile();
            if (treeImageFile.exists()) {
                treeImageFile.delete(); //rimuove vecchio albero
            }

            String apiKey = Files.readString(Path.of("src/main/resources/credentials.json")).trim(); //legge chiave api
            String jsonInput = "{\n" +
                "  \"document\": {\n" +
                "    \"type\": \"PLAIN_TEXT\",\n" +
                "    \"content\": \"" + inputText.replace("\\", "\\\\").replace("\"", "\\\"") + "\"\n" +
                "  },\n" +
                "  \"encodingType\": \"UTF8\"\n" +
                "}";

            //invio analisi a google
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://language.googleapis.com/v1/documents:analyzeSyntax?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInput))
                .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Files.writeString(inputJsonPath, response.body(), StandardCharsets.UTF_8); //salva json

            //compila il launcher
            ProcessBuilder pbCompile = new ProcessBuilder("javac", "--release", "17", "-encoding", "UTF-8", launcherPath.getFileName().toString());
            pbCompile.directory(syntaxTreeDir.toFile());
            pbCompile.inheritIO().start().waitFor();

            //esegue il launcher
            ProcessBuilder pbRun = new ProcessBuilder("java", "Launcher");
            pbRun.directory(syntaxTreeDir.toFile());
            pbRun.inheritIO().start().waitFor();

            //copia immagine prodotta nei path statici
            Path staticPath = Path.of("src/main/resources/static/tree.png");
            Files.copy(treeImagePath, staticPath, StandardCopyOption.REPLACE_EXISTING);

            Path runtimeStatic = Path.of("target/classes/static/tree.png");
            Files.createDirectories(runtimeStatic.getParent());
            Files.copy(treeImagePath, runtimeStatic, StandardCopyOption.REPLACE_EXISTING);

            return "OK"; //tutto ok
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Errore"; //errore generico
        }
    }
}
