package com.ProjectApp.NonSenseGenerator.english;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component //classe gestita da spring
public class EnglishSentenceBuilder {

    @Autowired
    private TemplateSelector templateSelector; //sceglie un template

    @Autowired
    private WordDictionaries dictionaries; //parole di backup

    @Autowired
    private GoogleTextModerationClient moderationClient; //verifica tossicità

    public String generateFromTokens(FilteredTokens tokens, int count) {
        StringBuilder result = new StringBuilder(); //frasi finali
        Random random = new Random(); //non usato direttamente

        for (int i = 1; i <= count; i++) {
            List<String> allTokens = new ArrayList<>();
            allTokens.addAll(tokens.nouns);
            allTokens.addAll(tokens.verbs);
            allTokens.addAll(tokens.adjectives);
            Collections.shuffle(allTokens); //mischia tutto

            Set<String> selected = new HashSet<>();
            for (int j = 0; j < Math.min(4, allTokens.size()); j++) {
                selected.add(allTokens.get(j)); //seleziona max 4 token
            }

            Queue<String> nouns = new LinkedList<>();
            Queue<String> verbs = new LinkedList<>();
            Queue<String> adjectives = new LinkedList<>();

            for (String word : selected) {
                if (tokens.nouns.contains(word)) nouns.add(word); //separa per tipo
                else if (tokens.verbs.contains(word)) verbs.add(word);
                else if (tokens.adjectives.contains(word)) adjectives.add(word);
            }

            String template = templateSelector.selectTemplate(tokens); //sceglie struttura frase
            Matcher matcher = Pattern.compile("\\{(\\w+)}").matcher(template); //placeholder
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String placeholder = matcher.group(1);
                String replacement;

                //riempie i placeholder
                switch (placeholder) {
                    case "Noun" -> replacement = nouns.isEmpty() ? dictionaries.getRandomNoun() : nouns.poll();
                    case "Verb" -> replacement = verbs.isEmpty() ? dictionaries.getRandomVerb() : verbs.poll();
                    case "VerbInfinitive" -> replacement = dictionaries.getRandomInfinitiveVerb(); //non serve aggiungere il "to" all'inizio perche' gia' presente nei template
                    case "VerbGerund" -> replacement = dictionaries.getRandomGerundVerb();
                    case "Adjective" -> replacement = adjectives.isEmpty() ? dictionaries.getRandomAdjective() : adjectives.poll();
                    case "Article" -> {
                        String nextWord = peekNextWord(template, matcher.end()); //prossima parola
                        replacement = dictionaries.getRandomArticle(nextWord); //usa articolo corretto
                    }
                    default -> replacement = ""; //fallback
                }

                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement)); //sostituzione sicura
            }

            matcher.appendTail(sb); //completa sostituzioni
            String sentence = cleanGrammar(sb.toString().trim()); //pulisce grammatica
            System.out.println(">> Checking toxicity: " + sentence); //debug
            String toxicity = moderationClient.moderateText(sentence); //controllo contenuto
            System.out.println(">> Result: " + toxicity); //debug
            result.append(i).append(". ").append(sentence).append(" [ Toxicity: ").append(toxicity).append(" ]<br>"); //aggiunge al risultato
        }

        return result.toString().trim(); //ritorna tutto
    }

    private String peekNextWord(String template, int fromIndex) {
        Matcher wordMatcher = Pattern.compile("\\{(\\w+)}").matcher(template); //cerca dopo current
        if (wordMatcher.find(fromIndex)) {
            return wordMatcher.group(1).toLowerCase(); //ritorna tipo
        }
        return ""; //se non trova
    }

    public String cleanGrammar(String s) {
        s = s.replaceAll("\\bto to\\b", "to"); //corregge doppio "to"
        s = s.replaceAll("\\s+", " ").trim(); //spazi multipli
        if (!s.endsWith(".")) {
            s += "."; //aggiunge punto
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1); //iniziale maiuscola
    }
}
