package com.example.wrd;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 8888;

    private ServerSocket serverSocket;
    private final Map<String, Lobby> lobbies;
    private final ExecutorService clientExecutor;
    private volatile boolean running;

    public GameServer() {
        lobbies = new ConcurrentHashMap<>();
        clientExecutor = Executors.newCachedThreadPool();
        running = false;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("Game Server started on port " + PORT);

            for (int i = 1; i <= 3; i++) {
                String lobbyId = "lobby" + i;
                lobbies.put(lobbyId, new Lobby(lobbyId, "Lobby " + i));
            }

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress());
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    clientExecutor.execute(handler);
                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            clientExecutor.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Lobby> getLobbies() {
        return lobbies;
    }

    public Lobby getLobby(String lobbyId) {
        return lobbies.get(lobbyId);
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }
}