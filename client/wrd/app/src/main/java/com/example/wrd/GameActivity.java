package com.example.wrd;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.graphics.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GameActivity extends AppCompatActivity {
    private GameView gameView;
    private TextView timerDisplay;
    private TextView statusDisplay;
    private EditText inputField;
    private Button readyButton;
    private Map<String, Player> players;
    private WordRound currentWordRound;
    private long roundEndTime;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private boolean hasLeftLobby = false;
    private boolean isReady = false;
    private static final long TYPING_THROTTLE_MS = 50;
    private long lastTypingSentTime = 0;
    private String lastSentText = "";
    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingTypingRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge and hide system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_game);

        players = new HashMap<>();

        gameView = findViewById(R.id.gameView);
        TextView wordDisplay = findViewById(R.id.wordDisplay);
        timerDisplay = findViewById(R.id.timerDisplay);
        statusDisplay = findViewById(R.id.statusDisplay);
        inputField = findViewById(R.id.inputField);
        readyButton = findViewById(R.id.readyButton);
        Button leaveButton = findViewById(R.id.leaveButton);

        timerHandler = new Handler(Looper.getMainLooper());

        inputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                sendTypingUpdateThrottled(text);
                // Check if typed text matches any unclaimed word
                if (currentWordRound != null && matchesUnclaimedWord(text)) {
                    submitAnswer();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        readyButton.setOnClickListener(v -> {
            if (!isReady) {
                isReady = true;
                readyButton.setEnabled(false);
                readyButton.setText("READY!");
                NetworkManager.getInstance().sendMessage(new Message(MessageType.PLAYER_READY, null));
            }
        });

        leaveButton.setOnClickListener(v -> leaveLobby());

        // Set up message listener
        NetworkManager.getInstance().setMessageListener(message ->
                runOnUiThread(() -> handleMessage(message))
        );

        // Request player list now that listener is ready
        NetworkManager.getInstance().sendMessage(new Message(MessageType.REQUEST_PLAYER_LIST, null));

        statusDisplay.setText("Click READY when you're ready!");
        readyButton.setVisibility(View.VISIBLE);
        inputField.setVisibility(View.GONE);
    }

    private void sendTypingUpdateThrottled(String text) {
        if (text.equals(lastSentText)) return;

        long now = System.currentTimeMillis();
        long timeSinceLastSend = now - lastTypingSentTime;

        if (pendingTypingRunnable != null) {
            typingHandler.removeCallbacks(pendingTypingRunnable);
            pendingTypingRunnable = null;
        }

        if (timeSinceLastSend >= TYPING_THROTTLE_MS) {
            sendTypingUpdate(text);
        } else {
            long delay = TYPING_THROTTLE_MS - timeSinceLastSend;
            pendingTypingRunnable = () -> sendTypingUpdate(text);
            typingHandler.postDelayed(pendingTypingRunnable, delay);
        }
    }

    private void sendTypingUpdate(String text) {
        lastSentText = text;
        lastTypingSentTime = System.currentTimeMillis();
        NetworkManager.getInstance().sendMessage(new Message(MessageType.TYPING_UPDATE, text));
    }

    private void handleMessage(Message message) {
        switch (message.getType()) {
            case PLAYER_LIST_UPDATE:
                Player[] playerArray = (Player[]) message.getData();
                players.clear();
                for (Player player : playerArray) {
                    players.put(player.getId(), player);
                }
                gameView.updatePlayers(players);
                break;

            case GAME_STARTING:
                int countdown = (int) message.getData();
                readyButton.setVisibility(View.GONE);
                timerDisplay.setTextColor(Color.parseColor("#4CAF50")); // Green for countdown
                timerDisplay.setText(String.valueOf(countdown));
                statusDisplay.setText("Game starting in " + countdown + "...");
                gameView.setCenterMessage("Starting in " + countdown + "...");
                break;

            case GAME_STARTED:
                statusDisplay.setText("Game Started!");
                timerDisplay.setText("");
                timerDisplay.setTextColor(Color.parseColor("#FF5252")); // Back to red for game timer
                gameView.clearCenterMessage();
                inputField.setVisibility(View.VISIBLE);
                inputField.setEnabled(true);
                break;

            case NEW_WORD:
                currentWordRound = (WordRound) message.getData();
                roundEndTime = System.currentTimeMillis() + currentWordRound.getTimeoutMs();

                // Clear any center message from previous round
                gameView.clearCenterMessage();

                // Update GameView with the word round (words shown in center)
                gameView.updateWordRound(currentWordRound);

                clearInputField();

                inputField.setEnabled(true);
                statusDisplay.setText("Type one of the words to claim it!");

                stopTimer();
                startTimer();
                inputField.post(() -> {
                    if (inputField.requestFocus()) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(inputField, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }
                });
                break;

            case WORD_CLAIMED:
                // Update the word round with claim info
                currentWordRound = (WordRound) message.getData();
                gameView.updateWordRound(currentWordRound);
                break;

            case REST_PERIOD:
                inputField.setEnabled(false);
                int restTime = (int) message.getData();
                statusDisplay.setText("Rest period...");
                stopTimer();
                // Keep words visible during rest so players can see who claimed what
                // Only start rest countdown (timer only, no center message)
                startRestCountdown(restTime);
                break;

            case TYPING_UPDATE:
            case SCORE_UPDATE:
                Player typingPlayer = (Player) message.getData();
                if (players.containsKey(typingPlayer.getId())) {
                    players.put(typingPlayer.getId(), typingPlayer);
                    gameView.updatePlayers(players);
                }
                break;

            case PLAYER_ELIMINATED:
                Player eliminatedPlayer = (Player) message.getData();
                if (players.containsKey(eliminatedPlayer.getId())) {
                    players.put(eliminatedPlayer.getId(), eliminatedPlayer);
                    gameView.updatePlayers(players);
                }
                gameView.setCenterMessage(eliminatedPlayer.getUsername() + " eliminated!");
                break;

            case GAME_ENDED:
                Player winner = (Player) message.getData();
                String winnerText = winner != null ? winner.getUsername() + " wins!" : "Game ended!";
                statusDisplay.setText(winnerText);
                inputField.setEnabled(false);
                stopTimer();
                gameView.clearWordRound();
                gameView.setCenterMessage(winnerText);
                break;

            case LEAVE_LOBBY_SUCCESS:
                hasLeftLobby = true;
                finish();
                break;
        }
    }
    private void clearInputField() {
        inputField.setText("");
        lastSentText = "";
    }

    private boolean matchesUnclaimedWord(String text) {
        if (currentWordRound == null) return false;
        java.util.List<String> words = currentWordRound.getWords();
        if (words == null) return false;

        for (int i = 0; i < words.size(); i++) {
            if (words.get(i).equals(text) && !currentWordRound.isWordClaimed(i)) {
                return true;
            }
        }
        return false;
    }

    private void startTimer() {
        timerDisplay.setTextColor(Color.parseColor("#FF5252")); // Red for game timer
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long remaining = roundEndTime - System.currentTimeMillis();
                if (remaining > 0) {
                    timerDisplay.setText(String.format(Locale.US, "%.1f", remaining / 1000.0));
                    timerHandler.postDelayed(this, 100);
                } else {
                    timerDisplay.setText("0.0");
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
        timerDisplay.setText("");
    }

    private void startRestCountdown(int restTimeMs) {
        timerDisplay.setTextColor(Color.parseColor("#4CAF50")); // Green for rest countdown
        final long restEndTime = System.currentTimeMillis() + restTimeMs;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long remaining = restEndTime - System.currentTimeMillis();
                if (remaining > 0) {
                    int seconds = (int) Math.ceil(remaining / 1000.0);
                    timerDisplay.setText(String.valueOf(seconds));
                    timerHandler.postDelayed(this, 100);
                } else {
                    timerDisplay.setText("");
                    timerDisplay.setTextColor(Color.parseColor("#FF5252")); // Back to red
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void submitAnswer() {
        if (pendingTypingRunnable != null) {
            typingHandler.removeCallbacks(pendingTypingRunnable);
            pendingTypingRunnable = null;
        }

        String answer = inputField.getText().toString();
        sendTypingUpdate(answer);

        NetworkManager.getInstance().sendMessage(new Message(MessageType.SUBMIT_ANSWER, answer));
        inputField.setEnabled(false);
    }

    private void leaveLobby() {
        if (hasLeftLobby) return;
        hasLeftLobby = true;

        new Thread(() -> {
            NetworkManager.getInstance().sendMessageSync(new Message(MessageType.LEAVE_LOBBY, null));
            runOnUiThread(this::finish);
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();

        if (pendingTypingRunnable != null) {
            typingHandler.removeCallbacks(pendingTypingRunnable);
        }

        if (!hasLeftLobby) {
            hasLeftLobby = true;
            new Thread(() ->
                    NetworkManager.getInstance().sendMessageSync(new Message(MessageType.LEAVE_LOBBY, null))
            ).start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                int[] location = new int[2];
                v.getLocationOnScreen(location);
                float x = event.getRawX();
                float y = event.getRawY();
                if (x < location[0] || x > location[0] + v.getWidth() ||
                        y < location[1] || y > location[1] + v.getHeight()) {
                    hideKeyboard();
                    v.clearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}