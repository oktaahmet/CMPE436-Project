package com.example.wrd;

import java.io.Serializable;

public record LobbyInfo(String id, String name, int currentPlayers, int maxPlayers,
                        boolean gameActive) implements Serializable {
    private static final long serialVersionUID = 436;

    @Override
    public String toString() {
        return name + " (" + currentPlayers + "/" + maxPlayers + ")" +
                (gameActive ? " [IN GAME]" : "");
    }
}