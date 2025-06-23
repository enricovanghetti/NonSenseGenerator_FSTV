package com.ProjectApp.NonSenseGenerator;

import com.ProjectApp.NonSenseGenerator.english.EnglishSentenceBuilder;
import com.ProjectApp.NonSenseGenerator.english.FilteredTokens;
import com.ProjectApp.NonSenseGenerator.english.InputAnalyzer;
import com.ProjectApp.NonSenseGenerator.english.TemplateSelector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class NonSenseGeneratorApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EnglishSentenceBuilder sentenceBuilder;

    @Autowired
    private TemplateSelector templateSelector;

    @Test
    public void contextLoads() {
        // Basic context load test
    }

    @Test
    public void homeEndpointShouldReturnBasicPage() {
        String response = this.restTemplate.getForObject("/", String.class);
        assertThat(response).contains("NonSenseGenerator");
    }

    @Test
    public void apiKeyPageShouldBeAccessible() {
        String response = this.restTemplate.getForObject("/apikey", String.class);
        assertThat(response).contains("API Key");
    }

    @Test
    public void shouldHandleGenerationErrorsGracefully() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("inputText", "");
        formData.add("sentenceCount", "2");

        String response = restTemplate.postForObject("/generate", formData, String.class);
        assertThat(response).contains("Error");
    }

    @Test
    public void shouldGenerateSyntaxTree() throws Exception {
        // Setup test credentials
        Path credentialsPath = Path.of("src/main/resources/credentials.json");
        Files.writeString(credentialsPath, "test-key");

        // Prepare request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, String> request = new HashMap<>();
        request.put("inputText", "Test sentence");
        
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        // Make request and verify
        String response = restTemplate.postForObject("/generateTree", entity, String.class);
        assertThat(response).isIn("OK", "Errore");
    }

    // Tests for EnglishSentenceBuilder
    @Test
    public void testSentenceBuilderGeneratesValidSentences() {
        FilteredTokens tokens = new FilteredTokens(
                List.of("cat", "dog"),
                List.of("run", "jump"),
                List.of("happy", "quick")
        );
        
        String result = sentenceBuilder.generateFromTokens(tokens, 2);
        assertNotNull(result);
        assertTrue(result.length() > 0);
        assertTrue(result.contains(".")); // Should end with period
    }

    @Test
    public void testSentenceBuilderHandlesEmptyTokens() {
        FilteredTokens emptyTokens = new FilteredTokens(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        
        String result = sentenceBuilder.generateFromTokens(emptyTokens, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testCleanGrammarFixesCommonIssues() {
        String dirty = "  to to go  to the  market  ";
        String cleaned = sentenceBuilder.cleanGrammar(dirty);
        assertEquals("To go to the market.", cleaned);
    }

    // Tests for InputAnalyzer
    @Test
    public void testAnalyzeFiltersBlacklistedWords() throws Exception {
        String text = "hello nigger world today tomorrow";
        FilteredTokens tokens = InputAnalyzer.analyze(text);
        
        assertFalse(tokens.nouns.contains("nigger"));
        assertFalse(tokens.nouns.contains("today"));
        assertFalse(tokens.nouns.contains("tomorrow"));
    }

    @Test
    public void testAnalyzeClassifiesPartsOfSpeech() throws Exception {
        String text = "quick brown fox jumps over lazy dog";
        FilteredTokens tokens = InputAnalyzer.analyze(text);
        
        assertTrue(tokens.nouns.contains("fox") || tokens.nouns.contains("dog"));
        assertTrue(tokens.verbs.contains("jumps"));
        assertTrue(tokens.adjectives.contains("quick") || tokens.adjectives.contains("brown") || tokens.adjectives.contains("lazy"));
    }

    @Test
    public void testCleanGrammarFixesArticles() {
        String incorrect1 = "a apple";
        String incorrect2 = "an book";
        
        String fixed1 = InputAnalyzer.cleanGrammar(incorrect1);
        String fixed2 = InputAnalyzer.cleanGrammar(incorrect2);
        
        assertEquals("An apple", fixed1);
        assertEquals("A book", fixed2);
    }

    // Tests for TemplateSelector
    @Test
    public void testTemplateSelectorReturnsValidTemplate() {
        FilteredTokens tokens = new FilteredTokens(
                List.of("cat"),
                List.of("runs"),
                List.of("happy")
        );
        
        String template = templateSelector.selectTemplate(tokens);
        assertNotNull(template);
        assertTrue(template.contains("{Noun}") || template.contains("{Verb}") || template.contains("{Adjective}"));
    }

    @Test
    public void testTemplateSelectorHandlesEmptyTokens() {
        FilteredTokens emptyTokens = new FilteredTokens(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        
        String template = templateSelector.selectTemplate(emptyTokens);
        assertNotNull(template);
        assertFalse(template.isEmpty());
    }
}
