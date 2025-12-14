package com.example.wrd;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WordRound implements Serializable {
    private static final long serialVersionUID = 436;

    private final List<String> words;
    private final int timeoutMs;
    // Track which words have been claimed (by player username)
    private final List<String> claimedBy;

    public WordRound(List<String> words, int timeoutMs) {
        this.words = new ArrayList<>(words);
        this.timeoutMs = timeoutMs;
        this.claimedBy = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            claimedBy.add(null); // null means not claimed
        }
    }

    public List<String> getWords() {
        return words;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public List<String> getClaimedBy() {
        return claimedBy;
    }

    public boolean isWordClaimed(int index) {
        return index >= 0 && index < claimedBy.size() && claimedBy.get(index) != null;
    }

    public String getClaimerAt(int index) {
        return index >= 0 && index < claimedBy.size() ? claimedBy.get(index) : null;
    }

    public void setClaimedBy(int index, String username) {
        if (index >= 0 && index < claimedBy.size()) {
            claimedBy.set(index, username);
        }
    }

    public int findWordIndex(String word) {
        for (int i = 0; i < words.size(); i++) {
            if (words.get(i).equals(word)) {
                return i;
            }
        }
        return -1;
    }
}