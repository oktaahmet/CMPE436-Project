package com.example.wrd;

import java.io.*;
import java.util.*;

public class WordGenerator {
    private static List<String> level1Words;
    private static List<String> level2Words;
    private static List<String> level3Words;
    private static List<String> level4Words;
    private static List<String> level5Words;
    private static boolean loaded = false;

    private final Random random;

    public WordGenerator() {
        random = new Random();
        if (!loaded) {
            loadWordsFromFile();
            loaded = true;
        }
    }

    private static void loadWordsFromFile() {
        level1Words = new ArrayList<>();
        level2Words = new ArrayList<>();
        level3Words = new ArrayList<>();
        level4Words = new ArrayList<>();
        level5Words = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader("words.txt"));
            String line;
            List<String> currentList = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equals("level1:")) {
                    currentList = level1Words;
                } else if (line.equals("level2:")) {
                    currentList = level2Words;
                } else if (line.equals("level3:")) {
                    currentList = level3Words;
                } else if (line.equals("level4:")) {
                    currentList = level4Words;
                } else if (line.equals("level5:")) {
                    currentList = level5Words;
                } else if (currentList != null && !line.endsWith(":")) {
                    currentList.add(line);
                }
            }
            reader.close();

            System.out.println("Loaded words - Level1: " + level1Words.size() +
                ", Level2: " + level2Words.size() +
                ", Level3: " + level3Words.size() +
                ", Level4: " + level4Words.size() +
                ", Level5: " + level5Words.size());

        } catch (IOException e) {
            System.out.println("Could not load words.txt, using default words");
            loadDefaultWords();
        }
    }

    private static void loadDefaultWords() {
        level1Words = Arrays.asList("Adventure", "Beautiful", "Dangerous", "Education", "Yesterday");
        level2Words = Arrays.asList("Accommodation", "Appreciation", "Controversial", "Exceptional");
        level3Words = Arrays.asList("Anticonstitutional", "Counterproductive", "Disproportionate");
        level4Words = Arrays.asList("Floccinaucinihilipilification", "Antidisestablishmentarianism");
        level5Words = List.of("Pneumonoultramicroscopicsilicovolcanoconiosis");
    }

    public List<String> getWords(int roundNumber, int count) {
        List<String> wordList = getWordListForRound(roundNumber);
        if (wordList.isEmpty()) {
            List<String> defaults = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                defaults.add("default" + (i + 1));
            }
            return defaults;
        }

        // Get 'count' unique random words
        List<String> result = new ArrayList<>();
        List<String> available = new ArrayList<>(wordList);

        for (int i = 0; i < count && !available.isEmpty(); i++) {
            int index = random.nextInt(available.size());
            result.add(available.remove(index));
        }

        // If we don't have enough unique words, allow duplicates with suffix
        while (result.size() < count) {
            String word = wordList.get(random.nextInt(wordList.size()));
            result.add(word);
        }

        return result;
    }

    private List<String> getWordListForRound(int roundNumber) {
        if (roundNumber <= 3) {
            return level1Words;
        } else if (roundNumber <= 6) {
            return level2Words;
        } else if (roundNumber <= 9) {
            return level3Words;
        } else if (roundNumber <= 12) {
            return level4Words;
        } else {
            return level5Words;
        }
    }
}