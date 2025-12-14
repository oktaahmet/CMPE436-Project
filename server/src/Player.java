package com.example.wrd;

import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 436;

    private final String id;
    private final String username;
    private int score;
    private int missCount;
    private boolean eliminated;
    private String currentTypedText;
    private String color;
    private boolean answeredCurrentRound;
    private boolean ready;

    public Player(String id, String username) {
        this.id = id;
        this.username = username;
        this.score = 0;
        this.missCount = 0;
        this.eliminated = false;
        this.currentTypedText = "";
        this.color = generateRandomColor();
        this.answeredCurrentRound = false;
        this.ready = false;
    }

    private String generateRandomColor() {
        String[] colors = {
            "#FF5252", "#E040FB", "#7C4DFF", "#536DFE",
            "#448AFF", "#40C4FF", "#18FFFF", "#64FFDA",
            "#69F0AE", "#B2FF59", "#EEFF41", "#FFD740"
        };
        return colors[(int) (Math.random() * colors.length)];
    }

    // Getters and setters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public void addScore(int points) { this.score += points; }

    public int getMissCount() { return missCount; }
    public void setMissCount(int missCount) { this.missCount = missCount; }
    public void incrementMissCount() { this.missCount++; }

    public boolean isEliminated() { return eliminated; }
    public void setEliminated(boolean eliminated) { this.eliminated = eliminated; }

    public String getCurrentTypedText() { return currentTypedText; }
    public void setCurrentTypedText(String text) { this.currentTypedText = text; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean hasAnsweredCurrentRound() { return answeredCurrentRound; }
    public void setAnsweredCurrentRound(boolean answered) { this.answeredCurrentRound = answered; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    @Override
    public String toString() {
        return "Player{" +
                "username='" + username + '\'' +
                ", score=" + score +
                ", eliminated=" + eliminated +
                '}';
    }
}
