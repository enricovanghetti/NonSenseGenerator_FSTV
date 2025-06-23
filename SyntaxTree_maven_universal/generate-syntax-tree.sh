#!/bin/bash

# seleziona versione di jq in base al sistema
unameOut="$(uname -s)"
if [[ "$unameOut" == "Linux" ]]; then
    JQ="./tools/jq-linux"
elif [[ "$unameOut" == "Darwin" ]]; then
    JQ="./tools/jq-macos"
else
    echo "Sistema non supportato"
    exit 1
fi

# acquisisce input JSON da stdin
echo "Incolla l'output JSON della Google NLP API (termina con CTRL+D):"
cat > input.json

# estrae solo i token rilevanti
echo "Estrazione token..."
$JQ '[.tokens[] | {content: .text.content, headTokenIndex: .dependencyEdge.headTokenIndex, pos: .partOfSpeech.tag}]' input.json > payload.json

# mostra il payload generato
echo "Payload JSON:"
cat payload.json

# invia al backend Spring Boot
echo "Invio al backend..."
curl -X POST http://localhost:8080/tree/generate \
     -H "Content-Type: application/json" \
     --data-binary "@payload.json" \
     --output tree.png

echo "Immagine salvata come tree.png"
