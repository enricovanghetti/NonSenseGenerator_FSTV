package com.ProjectApp.NonSenseGenerator.service;

import com.ProjectApp.NonSenseGenerator.english.EnglishSentenceBuilder;
import com.ProjectApp.NonSenseGenerator.english.FilteredTokens;
import com.ProjectApp.NonSenseGenerator.english.InputAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service //classe service spring
public class NonsenseService {

    @Autowired
    private EnglishSentenceBuilder sentenceBuilder; //genera frasi da token

    public String generateNonsense(String inputText, int sentenceCount) {
        try {
            FilteredTokens tokens = InputAnalyzer.analyze(inputText); //analizza frase
            return sentenceBuilder.generateFromTokens(tokens, sentenceCount); //genera output
        } catch (Exception e) {
            return "Error processing input: " + e.getMessage(); //errore analisi
        }
    }

    public String generateNonsenseFromTokens(FilteredTokens tokens, int sentenceCount) {
        try {
            return sentenceBuilder.generateFromTokens(tokens, sentenceCount); //usa token già filtrati
        } catch (Exception e) {
            return "Error generating from tokens: " + e.getMessage(); //errore generazione
        }
    }
}
