package com.example.moyiza_be.common.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class BadWordFiltering implements BadWords {
    private final Set<String> badWordsSet = new HashSet<>(List.of(badWords));
    private final Map<String, Pattern> badWordPatterns = new HashMap<>();

    @PostConstruct
    public void compileBadWordPatterns() {
        String patternText = buildPatternText();

        for (String word : badWordsSet) {
            String[] chars = word.split("");
            badWordPatterns.put(word, Pattern.compile(String.join(patternText, chars)));
        }
    }

    public boolean checkBadWord(String input) {
        for (Pattern pattern : badWordPatterns.values()) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }

    public String change(String text) {
        for (Map.Entry<String, Pattern> entry : badWordPatterns.entrySet()) {
            String word = entry.getKey();
            Pattern pattern = entry.getValue();
            if (word.length() == 1){
                text = text.replace(word, substituteValue);
            }
            text = pattern.matcher(text).replaceAll(matchedWord ->
                            substituteValue.repeat(matchedWord.group().length()));
        }
        return text;
    }

    private String buildPatternText() {
        StringBuilder patternBuilder = new StringBuilder("[");
        for (String delimiter : delimiters) {
            patternBuilder.append(Pattern.quote(delimiter));
        }
        patternBuilder.append("]*");
        return patternBuilder.toString();
    }
}