@echo off
setlocal EnableDelayedExpansion

:: usa jq locale
set JQ=tools\jq.exe

echo Incolla il JSON della Google NLP API e premi CTRL+Z poi INVIO:
copy con input.json

echo Estrazione token in formato compatibile...
%JQ% "[.tokens[] | {content: .text.content, headTokenIndex: .dependencyEdge.headTokenIndex, pos: .partOfSpeech.tag}]" input.json > payload.json

echo Payload JSON generato:
type payload.json

echo Invio richiesta al server Spring Boot...
curl -X POST http://localhost:8080/tree/generate ^
     -H "Content-Type: application/json" ^
     --data-binary "@payload.json" ^
     --output tree.png

echo Immagine salvata come tree.png
endlocal
