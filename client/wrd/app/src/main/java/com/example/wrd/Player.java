package com.example.wrd;
import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 436;
    private final String id;
    private final String username;
    private final int score;
    private final int missCount;
    private final boolean eliminated;
    private final String currentTypedText;
    private final String color;
    private final boolean ready;

    public Player(String id, String username) {
        this.id = id;
        this.username = username;
        this.score = 0;
        this.missCount = 0;
        this.eliminated = false;
        this.currentTypedText = "";
        this.color = generateRandomColor();
        this.ready = false;
    }

    private String generateRandomColor() {
        String[] colors = {
                "#FF5252", "#E040FB", "#7C4DFF", "#536DFE",
                "#448AFF", "#40C4FF", "#18FF9F", "#64FF9A",
                "#69F0AE", "#B2FF59", "#EEFF41", "#FFD740"
        };
        return colors[(int) (Math.random() * colors.length)];
    }

    // Getters and setters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public int getScore() { return score; }

    public int getMissCount() { return missCount; }

    public boolean isEliminated() { return eliminated; }

    public String getCurrentTypedText() { return currentTypedText; }

    public String getColor() { return color; }

    public boolean isReady() { return ready; }

    @Override
    public String toString() {
        return "Player{" +
                "username='" + username + '\'' +
                ", score=" + score +
                ", eliminated=" + eliminated +
                '}';
    }
}

