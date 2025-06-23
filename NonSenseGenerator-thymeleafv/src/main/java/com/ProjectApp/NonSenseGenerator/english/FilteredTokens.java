package com.ProjectApp.NonSenseGenerator.english;

import java.util.List;

public class FilteredTokens {
    public List<String> nouns; //nomi filtrati
    public List<String> verbs; //verbi filtrati
    public List<String> adjectives; //aggettivi filtrati

    public FilteredTokens(List<String> nouns, List<String> verbs, List<String> adjectives) {
        this.nouns = nouns;
        this.verbs = verbs;
        this.adjectives = adjectives;
    }

    public int totalWords() {
        return nouns.size() + verbs.size() + adjectives.size(); //conteggio totale
    }
}
