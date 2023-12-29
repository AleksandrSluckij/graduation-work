package searchengine.services.morphology;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MorphService {
    private static MorphService instance;
    private final LuceneMorphology morphology;
    private final String[] badForms = {"ПРЕДЛ", "СОЮЗ", "МЕЖД", "ЧАСТ"};

    public static MorphService getMorphService () {
        if (instance == null) {
            try {
                LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
                instance = new MorphService(luceneMorphology);
            } catch (IOException e) {
                instance = null;
            }
        }
        return instance;
    }

    private MorphService(LuceneMorphology luceneMorphology) {
        this.morphology = luceneMorphology;
    }

    public Map<String, Integer> getLemmas (String text) {
        Map<String, Integer> result = new HashMap<>();
        String[] words = getRussianWords(text);
        for (String word : words) {
            if (word.length() < 3) {
                continue;
            }
            if (notGoodForm(word)) {
                continue;
            }
            String baseForm = morphology.getNormalForms(word).get(0);
            if (result.containsKey(baseForm)) {
                result.put(baseForm, result.get(baseForm) + 1);
            } else {
                result.put(baseForm, 1);
            }
        }
        return result;
    }

    private String[] getRussianWords (String text) {
        String NON_RUSSIAN_SYMBOLS = "[^а-я\\s]";
        return text.toLowerCase().replaceAll(NON_RUSSIAN_SYMBOLS, " ").trim().split("\\s+");
    }

    private boolean notGoodForm (String word) {
        List<String> info = morphology.getMorphInfo(word);
        for (String part : info) {
            if (partIsNotGoodForm(part)) {
                return true;
            }
        }
        return false;
    }

    private boolean partIsNotGoodForm(String part) {
        for (String badForm : badForms) {
            if (part.toUpperCase().contains(badForm)) {
                return true;
            }
        }
        return false;
    }

}
