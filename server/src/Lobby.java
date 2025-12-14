package com.example.wrd;

import java.util.*;
import java.util.concurrent.*;

public class Lobby {
    private static final int MAX_PLAYERS = 8;
    private static final int MIN_PLAYERS_TO_START = 2;
    private static final int WORD_TIMEOUT = 13000; // 13 seconds
    private static final int REST_TIMEOUT = 5000; // 5 seconds
    private static final int MAX_MISSES = 2;
    private static final int MAX_ROUNDS = 16; // Game ends after 16 rounds
    private static final int GAME_END_DELAY = 10000; // 10 seconds before kicking players
    private static final int GAME_START_COUNTDOWN = 3; // 3 seconds countdown before game starts

    // Binary semaphore for mutual exclusion
    private final BinarySemaphore playerMutex = new BinarySemaphore(true);

    private final String id;
    private final String name;
    private final Map<String, Player> players;
    private final Map<String, ClientHandler> clientHandlers;
    private volatile boolean gameActive;
    private Thread gameThreadRunner;
    private GameThread gameThread;
    private final WordGenerator wordGenerator;

    // CountingSemaphore for tracking ready players
    private final CountingSemaphore readyPlayerSemaphore = new CountingSemaphore(0);

    // Scheduler for delayed kick
    private ScheduledExecutorService scheduler;

    public Lobby(String id, String name) {
        this.id = id;
        this.name = name;
        this.players = new ConcurrentHashMap<>();
        this.clientHandlers = new ConcurrentHashMap<>();
        this.gameActive = false;
        this.wordGenerator = new WordGenerator();
    }

    public boolean addPlayer(Player player, ClientHandler handler) {
        // Acquire mutex to modify players/clientHandlers safely
        playerMutex.P();
        try {
            if (players.size() >= MAX_PLAYERS || gameActive) {
                return false;
            }
            players.put(player.getId(), player);
            clientHandlers.put(player.getId(), handler);

            // initialize player fields
            player.setReady(false);
            player.setCurrentTypedText("");
            player.setScore(0);
            player.setMissCount(0);
            player.setEliminated(false);
        } finally {
            playerMutex.V();
        }
        broadcastPlayerList();
        return true;
    }

    public void setPlayerReady(Player player) {
        boolean shouldStart = false;

        playerMutex.P();
        try {
            if (gameActive || !players.containsKey(player.getId())) {
                return;
            }

            if (!player.isReady()) {
                player.setReady(true);
                readyPlayerSemaphore.V(); // Increment ready count
                int currentReady = readyPlayerSemaphore.getValue();
                System.out.println("Player " + player.getUsername() + " ready. Total ready: " + currentReady + "/" + players.size());
            }

            // determine whether we should start game
            int totalPlayers = players.size();
            int readyCount = readyPlayerSemaphore.getValue();
            if (totalPlayers >= MIN_PLAYERS_TO_START && readyCount == totalPlayers && !gameActive) {
                shouldStart = true;
            }
        } finally {
            playerMutex.V();
        }

        // broadcast and potentially start game outside mutex
        broadcastPlayerList();

        if (shouldStart) {
            startGame();
        }
    }

    public void removePlayer(Player player) {
        boolean shouldStop = false;
        boolean shouldStartAfterRemoval = false;

        // Acquire mutex to mutate structures safely
        playerMutex.P();
        try {
            // If player was ready, decrement ready count
            if (player.isReady()) {
                readyPlayerSemaphore.P(); // Decrement ready count
            }

            // Remove from maps
            players.remove(player.getId());
            clientHandlers.remove(player.getId());

            // After removal, decide if game must stop
            if (players.isEmpty() && gameActive) {
                shouldStop = true;
            } else {
                // Check if remaining players are all ready and can start the game
                int totalPlayers = players.size();
                int readyCount = readyPlayerSemaphore.getValue();
                if (totalPlayers >= MIN_PLAYERS_TO_START && readyCount == totalPlayers && !gameActive) {
                    shouldStartAfterRemoval = true;
                }
            }
        } finally {
            playerMutex.V();
        }

        // Broadcast outside mutex
        broadcastPlayerList();

        if (shouldStop) {
            stopGame();
        } else if (shouldStartAfterRemoval) {
            startGame();
        }
    }

    public void startGame() {
        // Acquire mutex so we can safely initialize player states
        playerMutex.P();
        try {
            if (gameActive || players.size() < MIN_PLAYERS_TO_START) {
                return;
            }

            gameActive = true;

            scheduler = Executors.newSingleThreadScheduledExecutor();

            for (Player player : players.values()) {
                player.setScore(0);
                player.setMissCount(0);
                player.setEliminated(false);
                player.setCurrentTypedText("");
                player.setReady(false);
                player.setAnsweredCurrentRound(false);
            }

            readyPlayerSemaphore.reset(0); // Reset ready count for next game

            gameThread = new GameThread();
            gameThreadRunner = new Thread(gameThread, "GameThread-" + id);
            gameThreadRunner.start();
        } finally {
            playerMutex.V();
        }
    }

    public void stopGame() {
        // We set flags and stop the thread
        if (!gameActive) return;

        // stop game thread
        gameActive = false;
        if (gameThread != null) {
            gameThread.stop();
        }
        if (gameThreadRunner != null && gameThreadRunner.isAlive()) {
            gameThreadRunner.interrupt();
        }

        // Broadcast outside locks
        broadcastMessage(new Message(MessageType.GAME_ENDED, null));

        // shutdown scheduler to avoid thread leak
        scheduler.shutdownNow();
    }

    // Kick all players from lobby
    private void kickAllPlayers() {
        System.out.println("Kicking all players from lobby " + id);

        // Send leave message to all players
        broadcastMessage(new Message(MessageType.LEAVE_LOBBY_SUCCESS, null));

        // Clear players and handlers
        playerMutex.P();
        try {
            players.clear();
            clientHandlers.clear();
            readyPlayerSemaphore.reset(0);
        } finally {
            playerMutex.V();
        }

        System.out.println("All players kicked from lobby " + id);

        // ensure scheduler not leaking
        scheduler.shutdownNow();
    }

    public void broadcastPlayerList() {
        Player[] playerArray;
        // snapshot players under mutex
        playerMutex.P();
        try {
            playerArray = players.values().toArray(new Player[0]);
        } finally {
            playerMutex.V();
        }

        Message message = new Message(MessageType.PLAYER_LIST_UPDATE, playerArray);
        broadcastMessage(message);
    }

    public void broadcastTypingUpdate(Player player) {
        Message message = new Message(MessageType.TYPING_UPDATE, player);
        broadcastMessage(message);
    }

    public void broadcastMessage(Message message) {
        List<ClientHandler> snapshot;
        // snapshot handlers under mutex to avoid concurrent changes while iterating
        playerMutex.P();
        try {
            snapshot = new ArrayList<>(clientHandlers.values());
        } finally {
            playerMutex.V();
        }

        // send outside mutex (network I/O should not run under internal locks)
        for (ClientHandler handler : snapshot) {
            try {
                handler.sendMessage(message);
            } catch (Exception e) {
                // log and continue; don't let one bad client break broadcasting
                e.printStackTrace();
            }
        }
    }

    public void submitAnswer(Player player, String answer) {
        if (!gameActive || player.isEliminated()) {
            return;
        }

        // forward to game thread if running
        GameThread gt = gameThread;
        if (gt != null) {
            gt.checkAnswer(player, answer);
        }
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getPlayerCount() {
        playerMutex.P();
        try {
            return players.size();
        } finally {
            playerMutex.V();
        }
    }
    public int getMaxPlayers() { return MAX_PLAYERS; }
    public boolean isGameActive() { return gameActive; }

    // Inner class for game logic
    private class GameThread implements Runnable {
        private volatile boolean running;
        private WordRound currentWordRound;
        private long roundStartTime;
        private int roundNumber;

        public GameThread() {
            this.running = true;
            this.roundNumber = 0;
        }

        @Override
        public void run() {
            try {
                // Countdown before game starts
                for (int i = GAME_START_COUNTDOWN; i > 0; i--) {
                    broadcastMessage(new Message(MessageType.GAME_STARTING, i));
                    Thread.sleep(1000);
                }
                broadcastMessage(new Message(MessageType.GAME_STARTED, null));

                while (running && gameActive) {
                    roundNumber++;

                    // Count active players to determine how many words to generate
                    int activePlayerCount;
                    playerMutex.P();
                    try {
                        activePlayerCount = (int) players.values().stream()
                                .filter(p -> !p.isEliminated())
                                .count();
                    } finally {
                        playerMutex.V();
                    }

                    // Generate n-1 words (one less than active players)
                    int wordCount = Math.max(1, activePlayerCount - 1);
                    java.util.List<String> words = wordGenerator.getWords(roundNumber, wordCount);

                    currentWordRound = new WordRound(words, WORD_TIMEOUT);
                    roundStartTime = System.currentTimeMillis();

                    Message message = new Message(MessageType.NEW_WORD, currentWordRound);
                    broadcastMessage(message);

                    System.out.println("Round " + roundNumber + "/" + MAX_ROUNDS + ": " + words + " (" + wordCount + " words for " + activePlayerCount + " players)");

                    Thread.sleep(WORD_TIMEOUT);

                    // check misses and update player state under playerLock
                    checkMisses();

                    // Count active (non-eliminated) players using a snapshot
                    long activePlayers;
                    playerMutex.P();
                    try {
                        activePlayers = players.values().stream()
                                .filter(p -> !p.isEliminated())
                                .count();
                    } finally {
                        playerMutex.V();
                    }

                    if (activePlayers <= 1 || roundNumber >= MAX_ROUNDS) {
                        endGame(activePlayers);
                        break;
                    }

                    // Rest period
                    Message restMessage = new Message(MessageType.REST_PERIOD, REST_TIMEOUT);
                    broadcastMessage(restMessage);
                    Thread.sleep(REST_TIMEOUT);
                }
            } catch (InterruptedException e) {
                System.out.println("Game thread interrupted");
            }
        }

        // synchronized to guard against concurrent claims, first come first served
        public synchronized void checkAnswer(Player player, String answer) {
            if (!running || player.isEliminated() || currentWordRound == null) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - roundStartTime;

            if (elapsed > WORD_TIMEOUT) {
                return;
            }

            // Check if player already answered this round
            if (player.hasAnsweredCurrentRound()) {
                return;
            }

            // Find the word in the list
            int wordIndex = currentWordRound.findWordIndex(answer);
            if (wordIndex == -1) {
                return; // Word not in the list
            }

            // Check if word is already claimed
            if (currentWordRound.isWordClaimed(wordIndex)) {
                return; // Word already taken by another player
            }

            // claim the word
            currentWordRound.setClaimedBy(wordIndex, player.getUsername());

            int remainingTime = (int) (WORD_TIMEOUT - elapsed);
            int points = Math.max(0, remainingTime / 10);

            // update player under mutex to be safe
            playerMutex.P();
            try {
                player.addScore(points);
                player.setMissCount(0);
                player.setAnsweredCurrentRound(true);
            } finally {
                playerMutex.V();
            }

            System.out.println("Player " + player.getUsername() + " claimed word '" + answer + "' and scored " + points + " points (total: " + player.getScore() + ")");

            // Broadcast score update
            Message scoreMessage = new Message(MessageType.SCORE_UPDATE, player);
            broadcastMessage(scoreMessage);

            // Broadcast word claimed update so all clients can see which words are taken
            Message claimedMessage = new Message(MessageType.WORD_CLAIMED, currentWordRound);
            broadcastMessage(claimedMessage);
        }

        private void checkMisses() {
            List<Message> eliminationMessages = new ArrayList<>();

            playerMutex.P();
            try {
                for (Player player : players.values()) {
                    if (!player.isEliminated() && !player.hasAnsweredCurrentRound()) {
                        player.incrementMissCount();

                        if (player.getMissCount() >= MAX_MISSES) {
                            player.setEliminated(true);
                            eliminationMessages.add(new Message(MessageType.PLAYER_ELIMINATED, player));
                            System.out.println("Player " + player.getUsername() + " eliminated");
                        }
                    }
                    player.setAnsweredCurrentRound(false);
                }
            } finally {
                playerMutex.V();
            }

            for (Message msg : eliminationMessages) {
                broadcastMessage(msg);
            }
        }

        private void endGame(long activePlayersCount) {
            running = false;
            gameActive = false;

            Player winner;
            // determine winner using snapshot under mutex
            playerMutex.P();
            try {
                if (activePlayersCount == 1) {
                    winner = players.values().stream()
                            .filter(p -> !p.isEliminated())
                            .findFirst()
                            .orElse(null);
                } else if (activePlayersCount == 0) {
                    winner = players.values().stream()
                            .max(Comparator.comparingInt(Player::getScore))
                            .orElse(null);
                } else {
                    winner = players.values().stream()
                            .filter(p -> !p.isEliminated())
                            .max(Comparator.comparingInt(Player::getScore))
                            .orElse(null);
                }
            } finally {
                playerMutex.V();
            }

            Message endMessage = new Message(MessageType.GAME_ENDED, winner);
            broadcastMessage(endMessage);

            System.out.println("Game ended after round " + roundNumber + ". Winner: " +
                    (winner != null ? winner.getUsername() + " (Score: " + winner.getScore() + ")" : "None"));

            // Schedule kick after GAME_END_DELAY
            scheduler.schedule(Lobby.this::kickAllPlayers, GAME_END_DELAY, TimeUnit.MILLISECONDS);
        }

        public void stop() {
            running = false;
        }
    }
}