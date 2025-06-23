package com.ProjectApp.NonSenseGenerator.english;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component //classe gestita da spring
public class TemplateSelector {

    private final List<String> templates; //lista di template frase
    private final ObjectMapper mapper = new ObjectMapper(); //parser json

    public TemplateSelector() throws Exception {
        InputStream input = new ClassPathResource("templates/sentence_templates_en.json").getInputStream(); //carica file
        templates = mapper.readValue(input, new TypeReference<List<Map<String, String>>>() {})
                .stream()
                .map(m -> m.get("template")) //estrae solo il campo template
                .toList();
    }

    public String selectTemplate(FilteredTokens tokens) {
        List<String> compatible = templates.stream()
                .filter(t -> templateMatches(t, tokens)) //filtra compatibili
                .toList();

        if (!compatible.isEmpty()) {
            return compatible.get(new Random().nextInt(compatible.size())); //template compatibile
        }

        return templates.get(new Random().nextInt(templates.size())); //fallback qualsiasi
    }

    private boolean templateMatches(String template, FilteredTokens tokens) {
        return countPlaceholders(template, "Noun") >= tokens.nouns.size()
            && (countPlaceholders(template, "Verb") + countPlaceholders(template, "VerbInfinitive") + countPlaceholders(template, "VerbGerund")) >= tokens.verbs.size()
            && countPlaceholders(template, "Adjective") >= tokens.adjectives.size(); //verifica copertura
    }

    private int countPlaceholders(String template, String placeholder) {
        Matcher matcher = Pattern.compile("\\{" + placeholder + "}").matcher(template); //trova segnaposti
        int count = 0;
        while (matcher.find()) count++; //conteggio
        return count;
    }
}
