package com.example.wrd;
import java.io.Serializable;

public class LobbyInfo implements Serializable {
    private static final long serialVersionUID = 436;
    private final String id;
    private final String name;
    private final int currentPlayers;
    private final int maxPlayers;
    private final boolean gameActive;

    public LobbyInfo(String id, String name, int currentPlayers, int maxPlayers, boolean gameActive) {
        this.id = id;
        this.name = name;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.gameActive = gameActive;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getCurrentPlayers() { return currentPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public boolean isGameActive() { return gameActive; }

    @Override
    public String toString() {
        return name + " (" + currentPlayers + "/" + maxPlayers + ")" +
                (gameActive ? " [IN GAME]" : "");
    }
}
