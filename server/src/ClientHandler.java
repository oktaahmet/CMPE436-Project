package com.example.wrd;

import java.util.UUID;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final GameServer server;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private Player player;
    private Lobby currentLobby;
    private volatile boolean running;
    private final Object outputLock = new Object();

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
        this.running = true;
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());

            while (running) {
                Message message = (Message) input.readObject();
                handleMessage(message);
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void handleMessage(Message message) {
        switch (message.getType()) {
            case JOIN_SERVER:
                handleJoinServer(message);
                break;
            case GET_LOBBIES:
                handleGetLobbies();
                break;
            case JOIN_LOBBY:
                handleJoinLobby(message);
                break;
            case LEAVE_LOBBY:
                handleLeaveLobby();
                break;
            case PLAYER_READY:
                handlePlayerReady();
                break;
            case TYPING_UPDATE:
                handleTypingUpdate(message);
                break;
            case SUBMIT_ANSWER:
                handleSubmitAnswer(message);
                break;
            case REQUEST_PLAYER_LIST:
                handleRequestPlayerList();
                break;
        }
    }

    private void handleJoinServer(Message message) {
        String username = (String) message.getData();
        player = new Player(generatePlayerId(), username);

        Message response = new Message(MessageType.JOIN_SERVER_RESPONSE, player);
        sendMessage(response);
        System.out.println("Player joined: " + username);
    }

    private void handleGetLobbies() {
        LobbyInfo[] lobbyInfos = server.getLobbies().values().stream()
            .map(lobby -> new LobbyInfo(
                lobby.getId(),
                lobby.getName(),
                lobby.getPlayerCount(),
                lobby.getMaxPlayers(),
                lobby.isGameActive()
            ))
            .toArray(LobbyInfo[]::new);

        Message response = new Message(MessageType.LOBBY_LIST, lobbyInfos);
        sendMessage(response);
    }

    private void handleJoinLobby(Message message) {
        String lobbyId = (String) message.getData();
        Lobby lobby = server.getLobby(lobbyId);

        if (lobby != null && lobby.addPlayer(player, this)) {
            currentLobby = lobby;
            Message response = new Message(MessageType.JOIN_LOBBY_SUCCESS, lobbyId);
            sendMessage(response);
            System.out.println("Player " + player.getUsername() + " joined lobby " + lobbyId);
        } else {
            Message response = new Message(MessageType.JOIN_LOBBY_FAILED, "Lobby is full or game in progress");
            sendMessage(response);
        }
    }

    private void handleLeaveLobby() {
        if (currentLobby != null) {
            System.out.println("Player " + player.getUsername() + " left lobby " + currentLobby.getId());
            currentLobby.removePlayer(player);
            currentLobby = null;
            Message response = new Message(MessageType.LEAVE_LOBBY_SUCCESS, null);
            sendMessage(response);
        }
    }

    private void handlePlayerReady() {
        if (currentLobby != null && player != null) {
            System.out.println("Player " + player.getUsername() + " is ready");
            currentLobby.setPlayerReady(player);
        }
    }

    private void handleTypingUpdate(Message message) {
        if (currentLobby != null) {
            String typedText = (String) message.getData();
            player.setCurrentTypedText(typedText);
            currentLobby.broadcastTypingUpdate(player);
        }
    }

    private void handleRequestPlayerList() {
        if (currentLobby != null) {
            currentLobby.broadcastPlayerList();
        }
    }

    private void handleSubmitAnswer(Message message) {
        if (currentLobby != null) {
            String answer = (String) message.getData();
            currentLobby.submitAnswer(player, answer);
        }
    }

    public void sendMessage(Message message) {
        synchronized (outputLock) {
            try {
                output.writeObject(message);
                output.flush();
                output.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void cleanup() {
        running = false;
        if (currentLobby != null) {
            currentLobby.removePlayer(player);
        }
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generatePlayerId() {
        return "player_" + UUID.randomUUID();
    }
}