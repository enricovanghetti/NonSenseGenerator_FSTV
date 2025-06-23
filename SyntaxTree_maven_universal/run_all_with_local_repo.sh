#!/bin/bash

mkdir -p tools

# rileva sistema operativo
unameOut="$(uname -s)"
archOut="$(uname -m)"

# determina path per jq
if [[ "$unameOut" == "Linux" ]]; then
    JQ="./tools/jq-linux"
    if [ ! -f "$JQ" ]; then
        echo "Scaricamento jq per Linux..."
        curl -L -o "$JQ" https://github.com/stedolan/jq/releases/latest/download/jq-linux64
        chmod +x "$JQ"
    fi
elif [[ "$unameOut" == "Darwin" ]]; then
    JQ="./tools/jq-macos"
    if [ ! -f "$JQ" ]; then
        echo "Scaricamento jq per macOS..."
        curl -L -o "$JQ" https://github.com/stedolan/jq/releases/latest/download/jq-osx-amd64
        chmod +x "$JQ"
    fi
else
    echo "Sistema non supportato"
    exit 1
fi

# compila ed esegue semplificatore
echo "Esecuzione del semplificatore JSON..."
chmod +x mvnw
./mvnw compile exec:java \
  -Dexec.mainClass=com.example.syntaxtree.SimplifySyntaxJson \
  -Dexec.args="input.json output.json" || { echo "Errore nel semplificatore"; exit 1; }

# controlla se la porta è già occupata
if lsof -i :8081 >/dev/null 2>&1; then
    echo "Porta 8081 già occupata. Salto avvio server."
    SERVER_PID=""
else
    echo "Avvio server Spring Boot (porta 8081)..."
    ./mvnw spring-boot:run \
      -Dspring-boot.run.mainClass=com.example.syntaxtree.SyntaxTreeApplication \
      -Dspring-boot.run.arguments="--server.port=8081" &
    SERVER_PID=$!
    sleep 3
fi

# invia JSON per generare tree.png
echo "Generazione dell'albero sintattico PNG..."
curl -X POST http://localhost:8081/tree/generate \
     -H "Content-Type: application/json" \
     --data-binary "@output.json" \
     --output tree.png

# attende generazione effettiva immagine
TARGET_STATIC="../NonSenseGenerator-thymeleafv/target/classes/static/tree.png"
echo "Attendo la creazione dell'immagine in: $TARGET_STATIC..."
for i in {1..2}; do
    if [[ -f "$TARGET_STATIC" ]]; then
        echo "Immagine rilevata!"
        break
    fi
    sleep 1
done

# arresta il server solo se è stato avviato
if [[ -n "$SERVER_PID" ]]; then
    echo "Arresto server Spring Boot..."
    kill "$SERVER_PID" 2>/dev/null || echo "Nessun processo da terminare"
fi

echo "Immagine salvata e visibile sul sito!"
