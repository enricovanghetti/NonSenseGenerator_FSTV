import java.io.*;
import java.nio.file.*;

public class Launcher {
    public static void main(String[] args) {
        String os = System.getProperty("os.name").toLowerCase();
        String jqPath = "";
        String jqDownloadUrl = "";

        try {
            //rilevamneto OS
            if (os.contains("win")) {
                System.out.println("Avvio su Windows...");
                jqPath = "tools\\jq.exe";
                jqDownloadUrl = "https://github.com/stedolan/jq/releases/latest/download/jq-win64.exe";
                ensureJqExists(jqPath, jqDownloadUrl);
                executeCommand(new ProcessBuilder("cmd.exe", "/c", "run_all_with_local_repo.bat"));

            } else if (os.contains("mac")) {
                System.out.println("Avvio su macOS...");
                jqPath = "./tools/jq-macos";
                jqDownloadUrl = "https://github.com/stedolan/jq/releases/latest/download/jq-osx-amd64";
                ensureJqExists(jqPath, jqDownloadUrl);
                executeCommand(new ProcessBuilder("bash", "run_all_with_local_repo.sh"));

            } else if (os.contains("nix") || os.contains("nux")) {
                System.out.println("Avvio su Linux...");
                jqPath = "./tools/jq-linux";
                jqDownloadUrl = "https://github.com/stedolan/jq/releases/latest/download/jq-linux64";
                ensureJqExists(jqPath, jqDownloadUrl);
                executeCommand(new ProcessBuilder("bash", "run_all_with_local_repo.sh"));

            } else {
                System.err.println("Sistema operativo non supportato: " + os);
            }

            System.out.println("Variabile JQ = " + jqPath);

        } catch (IOException | InterruptedException e) {
            System.err.println("Errore in Launcher:");
            e.printStackTrace();
        }
    }

    // scarica jq se non esiste
    private static void ensureJqExists(String jqPath, String downloadUrl) throws IOException {
        Path jqFile = Paths.get(jqPath);
        if (!Files.exists(jqFile)) {
            System.out.println("Scaricamento jq da: " + downloadUrl);
            Files.createDirectories(jqFile.getParent());
            try (InputStream in = new java.net.URL(downloadUrl).openStream()) {
                Files.copy(in, jqFile, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!jqPath.endsWith(".exe")) {
                jqFile.toFile().setExecutable(true); //rende eseguibile su unix
            }
        } else {
            System.out.println("jq già presente in: " + jqPath);
        }
    }

    // esegue comando da terminale
    private static void executeCommand(ProcessBuilder builder) throws IOException, InterruptedException {
        builder.redirectErrorStream(true);
        Process process = builder.start();

        System.out.println("Avvio comando: " + String.join(" ", builder.command()));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("  > " + line); //output in streaming
            }
        }

        int exitCode = process.waitFor();
        System.out.println("Comando terminato con codice: " + exitCode);

        if (exitCode != 0) {
            System.err.println("Il comando ha restituito un codice di errore.");
        }
    }
}
