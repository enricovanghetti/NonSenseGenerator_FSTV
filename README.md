## **Introduzione**

La web app **NonSenseGenerator** permette di generare frasi in lingua inglese grammaticalmente e sintatticamente corrette ma senza un significato logico coerente. La generazione può avvenire da zero oppure fornendo una frase di input (nella stessa lingua) dalla quale il programma preleverà elementi per inserirli nelle frasi da generare, permettendo così un grado parziale di personalizzazione. Il numero di frasi da generare varia da 1 a 9 in base alla scelta dell'utente. Su richiesta, la piattaforma permette anche di mostrare l'albero sintattico relativo alla frase di input ed anche di copiare in blocco tutte le frasi generate. Infine, per ogni frase generata è stampato anche il suo status di tossicità, qualora ve ne fosse (è stato gia implementato un filtro sui termini della frase in input).

## **Tecnologie Usate**

|Nome                  |Versione  |Descrizione                                                                       |
|:--------------------:|:--------:|:---------------------------------------------------------------------------------|
|Java                  |17        |Ambiente e linguaggio di programmazione, retrocompatibilità per versioni successive|    
|JGraphX               |4.0       |Set di librerie Java per visualizzione diagrammi e grafi                          |
|SpringBoot            |3.2.5     |Framework Java OpenSource per creazione di applicazioni autonome basate su Spring |
|Thymeleaf             |3.2.5     |Motore di template Java XML/HTML adatto per applicazioni Web basate su MVC        |
|JUnit                 |5.10.2    |Framework per testing automatico delle classi Java                                |
|Google Cloud Language |2.36.0    |API per funzioni di Natural Language Processing                                   |
|Gson                  |2.10.1    |Libreria Java per conversione JSON a Java Object e viceversa                      |
|Jq                    |1.7.1     |JSON parser/editor per riga di comando                                            |



## **Descrizione ed Implementazione Progetto**

### Analisi della frase di input

Viene gestita dalla classe `InputAnalyzer` di tipo Spring `@Component`, in particolare dalla funzione `analyze(String)`. Dopo aver letto la API key dal file _"src/resources/credentials.json"_ invia una richiesta HTTP al Google Cloud NLP server del metodo `analyzeSyntax()` che fornisce una risposta in formato JSON dalla quale vengono estratti i token con tag VERB, ADJ, NOUN relativi al JSON Object `partOfSpeech`, che poi vengono salvati nelle rispettive `List<String>` usate nella classe `FilteredTokens`. In caso di frase di input vuota vengono scelti dai dizionari già presenti come risorsa statica alcuni elementi in modo da permettere comunque la generazione di frasi. 

### Selezione del/i template di frase

Gestita dalla classe `TemplateSelector` di tipo Spring `@Component`. Il costruttore preleva il file JSON *"templates/sentence_templates_en.json"* contenente i vari template di frase forniti di segnaposti (racchiusi in {}) per verbi, aggettivi e sostantivi. La funzione `selectTemplate(FilteredTokens)` scorre la lista del file JSON di template prima specificato e returna uno o più template randomici adatto/i al numero di sostantivi, verbi e aggettivi già presenti nella frase di input, selezionandoli però da una `List<String>` di template compatibili grazie alla funzione predicativa booleana `templateMatches(String, FilteredTokens)`. A sua volta quest'ultima fa uso della funzione `countPlaceholders(String,String)` per contare quanti segnaposto di ogni tipo (verbo, sostantivo, aggettivo) presenta il template in fase di selezione.

### Determinazione tossicità frase/i generate/i

Gestita dalla classe `GoogleTextModerationClient`, la cui funzione principale `moderateText(String)` invoca una richiesta HTTP al Google Cloud NLP server relativa al metodo `moderateText()` la cui risposta in formato JSON viene filtrata estraendo solo il valore relativo al livello di tossicità del parametro stringa passato. In particolare se i valori relativi a questo aspetto soddisfano una certa soglia abbiamo considerato la stringa come _"Clean"_.

### Costruzione frase/i in output

Gestita dalla classe `EnglishSentenceBuilder`, sfrutta tutte le sopra citate classi e relative funzioni per creare una frase "nonsense" in base ai dati raccolti dalla frase di input. In particolare, la funzione principale `generateTokens(FilteredTokens, int)` sceglie un numero definito di token della frase di input, aggiungendo ognuno in un arrary contenente entità dello stesso valore sintattico; sceglie un template di frase adatto al numero di tokens prelevati, sostituisce i placeholder con le entità prelevate dall'input (oppure in caso di mancanza vengono scelte da dei dizionari già presenti in formato JSON) ed infine effettua un controllo della tossicità della frase cosi prodotta.

> Le chiamate a funzione effettive di `analyze(String)` in `InputAnalyzer` e di `generateTokens(FilteredTokens, int)` in `EnglishSentenceBuilder` vengono fatte all'interno della classe `NonsenseService` di tipo Spring `@Service` (una specializzazione di `@Component` che identifica una classe di implementazione)

### Costruzione dell'albero sintattico
Gestita dall'applicazione SpringBoot ausiliaria della cartella */SyntaxTree_maven_universal*. Ogni volta che viene richiesto l'albero sintattico della frase inserita viene fatto partire un altro server SpringBoot su una porta differente da quella del server principale, nel quale si effettua la chiamata alla funzione `main` della classe `SimplifySyntaxJson` per estrarre dal file JSON della risposta alla chiamata di `analyzeSyntax()` del Google Cloud NLP API (salvata in *SyntaxTree_maven_universal/input.json*) solo i campi relativi alla posizione degli indici di parentela della morfologia della frase, contenuti in `headTokenIndex`. Il file così semplificato viene salvato in *SyntaxTree_maven_universal/output.json* e le informazioni usate dalle funzioni di JGraphX per la creazione dell'albero 

## Albero delle directory del progetto

```
NonSenseGenerator-thymeleafv/
├─ src/
│  ├─ main/
│  │  ├─ java/com/ProjectApp/NonSenseGenerator/
│  │  │  ├─ english/
│  │  │  │  ├─ EnglishSentenceBuiler.java
│  │  │  │  ├─ FilteredTokens.java
│  │  │  │  ├─ TemplateSelector.java
│  │  │  │  ├─ InputAnalyzer.java
│  │  │  │  ├─ WordDictionaries.java
│  │  │  ├─ service/
│  │  │  │  ├─ NonsenseService.java
│  │  │  ├─ Application.java
│  │  │  ├─ NSGController.java
│  │  ├─ resources/
│  │  │  ├─ dictionaries_en/
│  │  │  │  ├─ adjectives.json
│  │  │  │  ├─ articles.json
│  │  │  │  ├─ nouns.json
│  │  │  │  ├─ verbs-gerun.json
│  │  │  │  ├─ verbs-infinitive.json
│  │  │  ├─ static/
│  │  │  │  ├─ css/
│  │  │  │  │  ├─ style.css
│  │  │  │  ├─ js/
│  │  │  │  │  ├─ script.js
│  │  │  ├─ templates/
│  │  │  │  ├─ apikey.html
│  │  │  │  ├─ NSG.html
│  │  │  │  ├─ sentence_templates_en.json
│  │  │  ├─ application.properties
│  │  │  ├─ application-test.properties
│  │  │  ├─ credentials.json
│  ├─ test/java/com/ProjectApp/NonSenseGenerator/
│  │  ├─ NonSenseGeneratorApplicationTests.java
├─ mvnw.xml
├─ mvnw.cmd
├─ pom.xml
```
```
SyntaxTree_maven_universal/
├─ custom-settings/
│  ├─ settings.xml
├─ src/
│  ├─ main/
│  │  ├─ java/com/example/syntaxtree/ 
│  │  │  ├─ Application.java
│  │  │  ├─ SimplifySyntaxJson.java
│  │  │  ├─ SyntaxTreeController.java
│  │  ├─ resources/
│  │  │  ├─ application.properties
├─ tools/
│  ├─ .exe di versioni OS specifiche di jq
├─ input.json
├─ output.json
├─ pom.xml
├─ mvnw.xml
├─ mvnw.cmd
├─ Launcher.java
├─ Launcher.class
├─ generate-syntax-tree.bat
├─ generate-syntax-tree.sh
├─ run_all_with_local_repo.bat
├─ run_all_with_local_repo.sh
README.md
```
> *generate-syntax-tree.bat* e *generate-syntax-tree.sh* sono script per terminale (cmd o bash/bash-like) necessari per eseguire Jq sul file Json avente le informazioni della frase di input analizzata, per poi salvare l'immagine dell'albero sintattico relativo in formato .png in locale e caricarla sulla piattaforma. *run_all_with_local_repo.bat* e *run_all_with_local_repo.sh* sono invece script per terminale per eseguire in automatico i comandi per avviare il server SpringBoot in caso di richiesta dell'albero sintattico. Infine *Launcher.java* è un file sorgente Java in grado di scaricare la versione di Jq adatta al sistema operativo utente.


## **Download ed avvio della piattaforma**

- Dopo aver clonato le repository, da terminale, posizionarsi nella cartella *"/NonSenseGenerator-thymeleafv"* 
- Eseguire il comando `mvn spring-boot:run` per avviare il processo di build dei file sorgente ed avvio del server SpringBoot
- In caso di packages mancanti, eseguire il comando `mvn clean install` per scaricare tutte le componenti necessarie in automatico e ripetere il punto precedente
- La piattaforma sarà disponibile in locale, alla porta 8080; per accedervi basterà andare in un qualunque browser ed immettere nella barra di ricerca `http://localhost:8080` 


## Report di Unit Testing

Disponibile nel sito --> [Unit Tests](https://lourenzi.github.io/NSG/TestResults.html)

## Note

Anche se in compilazione ed esecuzione "filerà tutto liscio", nel terminale potrebbe comparire una serie di errori, ma questi sono inevitabili:
1. Il server SyntaxTreeApplication viene ucciso manualmente dallo script dopo la generazione (`taskkill /PID !TARGET_PID! /F`)
2. Maven interpreta la terminazione forzata del processo Spring Boot come un errore, anche se in realtà è voluta
