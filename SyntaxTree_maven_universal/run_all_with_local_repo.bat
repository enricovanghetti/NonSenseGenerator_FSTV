@echo off
setlocal enabledelayedexpansion

echo Avvio NonSenseGenerator su Windows...

REM crea cartella tools se non esiste
if not exist tools (
    mkdir tools
)

REM scarica jq.exe se non presente
if not exist tools\jq.exe (
    echo Scaricamento jq.exe...
    curl -L -o tools\jq.exe https://github.com/stedolan/jq/releases/latest/download/jq-win64.exe
)

set JQ=tools\jq.exe

REM esecuzione semplificatore JSON
echo Esecuzione del semplificatore JSON...
call mvnw.cmd compile exec:java ^
  -Dexec.mainClass=com.example.syntaxtree.SimplifySyntaxJson ^
  -Dexec.args="input.json output.json"
IF ERRORLEVEL 1 (
    echo Errore nel semplificatore
    exit /b 1
)

REM controlla se la porta 8081 è già usata
for /f "tokens=5" %%a in ('netstat -aon ^| find ":8081" ^| find "LISTENING"') do (
    set PORT_IN_USE=1
)
if defined PORT_IN_USE (
    echo Porta 8081 già occupata. Salto avvio server.
    set SERVER_STARTED=0
) else (
    echo Avvio server Spring Boot su porta 8081...
    start "" /B cmd /c "call mvnw.cmd spring-boot:run -Dserver.port=8081"
    set SERVER_STARTED=1
)

REM attesa risposta dal server
echo Attesa disponibilità del server su /tree/generate...
set SERVER_READY=0
for /L %%i in (1,1,10) do (
    curl -s -X OPTIONS http://localhost:8081/tree/generate >nul 2>&1
    if !errorlevel! neq 7 (
        set SERVER_READY=1
        echo Server pronto!
        goto :send_request
    )
    echo Tentativo %%i/10: server non ancora disponibile...
    timeout /t 1 >nul
)

echo Timeout: il server non ha risposto.
goto :stop_server

:send_request
REM invia output.json al backend
echo Generazione dell'albero sintattico PNG...
curl -s -X POST http://localhost:8081/tree/generate ^
     -H "Content-Type: application/json" ^
     --data-binary "@output.json" ^
     --output tree.png

REM verifica se tree.png è stato generato nella cartella static
set TARGET_STATIC=..\NonSenseGenerator-thymeleafv\target\classes\static\tree.png
echo Attendo la creazione dell'immagine in: %TARGET_STATIC%
set FOUND=0
for /L %%i in (1,1,5) do (
    if exist "%TARGET_STATIC%" (
        echo Immagine rilevata!
        set FOUND=1
        goto :stop_server
    )
    timeout /t 1 >nul
)

if "!FOUND!"=="0" (
    echo Immagine non trovata entro il tempo previsto.
)

:stop_server
REM arresta server solo se è stato avviato da questo script
if "!SERVER_STARTED!"=="1" (
    echo Arresto server Spring Boot su porta 8081...
    set TARGET_PID=
    for /f "tokens=5" %%a in ('netstat -aon ^| find ":8081" ^| find "LISTENING"') do (
        set TARGET_PID=%%a
    )

    if defined TARGET_PID (
        echo Terminazione PID !TARGET_PID!...
        taskkill /PID !TARGET_PID! /F >nul 2>&1
    ) else (
        echo Nessun processo da terminare (porta 8081 non attiva)
    )
)

echo Immagine salvata e visibile sul sito!
endlocal
