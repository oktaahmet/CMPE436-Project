package com.example.wrd;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameView extends View {
    private Paint circlePaint;
    private Paint playerPaint;
    private Paint textPaint;
    private Paint smallTextPaint;
    private Paint wordPaint;
    private Paint claimedWordPaint;
    private Paint claimerTextPaint;
    private List<Player> players;
    private float centerX;
    private float centerY;
    private float mainCircleRadius;
    private float playerCircleRadius;

    // Word racing data
    private WordRound currentWordRound;

    // Center message for eliminations/winner
    private String centerMessage;
    private Paint centerMessagePaint;

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        players = new ArrayList<>();

        // Enable hardware acceleration
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor("#7A3F3F"));
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(15);

        playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        playerPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(48);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        smallTextPaint.setColor(Color.parseColor("#CCCCCC"));
        smallTextPaint.setTextSize(36);
        smallTextPaint.setTextAlign(Paint.Align.CENTER);

        // Paint for available words in center
        wordPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wordPaint.setColor(Color.parseColor("#FFD700")); // Gold color for available words
        wordPaint.setTextSize(42);
        wordPaint.setTextAlign(Paint.Align.CENTER);
        wordPaint.setFakeBoldText(true);

        // Paint for claimed words (strikethrough style, grayed out)
        claimedWordPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        claimedWordPaint.setColor(Color.parseColor("#666666")); // Gray for claimed
        claimedWordPaint.setTextSize(42);
        claimedWordPaint.setTextAlign(Paint.Align.CENTER);
        claimedWordPaint.setStrikeThruText(true);

        // Paint for showing who claimed a word
        claimerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        claimerTextPaint.setColor(Color.parseColor("#4CAF50")); // Green for claimer name
        claimerTextPaint.setTextSize(28);
        claimerTextPaint.setTextAlign(Paint.Align.CENTER);

        // Paint for center messages (eliminations, winner)
        centerMessagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerMessagePaint.setColor(Color.parseColor("#FF6B6B")); // Soft red
        centerMessagePaint.setTextSize(48);
        centerMessagePaint.setTextAlign(Paint.Align.CENTER);
        centerMessagePaint.setFakeBoldText(true);
    }

    public void updatePlayers(Map<String, Player> playerMap) {
        players.clear();
        players.addAll(playerMap.values());
        invalidate();
    }

    public void updateWordRound(WordRound wordRound) {
        this.currentWordRound = wordRound;
        invalidate();
    }

    public void clearWordRound() {
        this.currentWordRound = null;
        invalidate();
    }

    public void setCenterMessage(String message) {
        this.centerMessage = message;
        invalidate();
    }

    public void clearCenterMessage() {
        this.centerMessage = null;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateDimensions(w, h);
    }

    private void updateDimensions(int w, int h) {
        if (w > 0 && h > 0) {
            centerX = w / 2f;
            centerY = h / 2f;
            mainCircleRadius = Math.min(w, h) * 0.40f;
            playerCircleRadius = 60;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Ensure dimensions are set
        if (centerX == 0 || centerY == 0) {
            updateDimensions(getWidth(), getHeight());
        }

        canvas.drawColor(Color.parseColor("#212121"));

        if (mainCircleRadius > 0) {
            canvas.drawCircle(centerX, centerY, mainCircleRadius, circlePaint);
        }

        // Draw words in the center of the table
        drawWordsInCenter(canvas);

        // Draw players around the circle
        int playerCount = players.size();
        if (playerCount == 0) return;

        double angleStep = (2 * Math.PI) / playerCount;

        for (int i = 0; i < playerCount; i++) {
            Player player = players.get(i);
            double angle = i * angleStep - Math.PI / 2;

            float x = centerX + (float) (mainCircleRadius * Math.cos(angle));
            float y = centerY + (float) (mainCircleRadius * Math.sin(angle));

            try {
                playerPaint.setColor(Color.parseColor(player.getColor()));
            } catch (Exception e) {
                playerPaint.setColor(Color.BLUE);
            }

            // Draw player circle with opacity based on elimination status
            if (player.isEliminated()) {
                playerPaint.setAlpha(100); // Faded if eliminated
            } else {
                playerPaint.setAlpha(255); // Full opacity
            }

            canvas.drawCircle(x, y, playerCircleRadius, playerPaint);

            // Draw username above player circle
            String displayName = player.getUsername();
            if (player.isReady()) {
                displayName += " ✓";
            }
            canvas.drawText(displayName, x, y - playerCircleRadius - 15, textPaint);

            String typedText = player.getCurrentTypedText();
            if (typedText == null || typedText.isEmpty()) {
                typedText = "...";
            }
            Paint typingPaint = new Paint(smallTextPaint);
            typingPaint.setColor(Color.parseColor("#00FF00")); // Green for typing
            canvas.drawText(typedText, x, y + playerCircleRadius + 40, typingPaint);

            // Draw score below typed text
            String scoreText = ""+player.getScore();
            canvas.drawText(scoreText, x, y + playerCircleRadius + 75, smallTextPaint);

            // Draw miss count if any
            if (player.getMissCount() > 0) {
                String missText = "Misses: " + player.getMissCount() + "/2";
                Paint missPaint = new Paint(smallTextPaint);
                missPaint.setColor(Color.RED);
                canvas.drawText(missText, x, y + playerCircleRadius + 105, missPaint);
            }
        }
    }

    private void drawWordsInCenter(Canvas canvas) {
        // Draw center message if present (takes priority)
        if (centerMessage != null && !centerMessage.isEmpty()) {
            canvas.drawText(centerMessage, centerX, centerY, centerMessagePaint);
            return;
        }

        if (currentWordRound == null) return;

        List<String> words = currentWordRound.getWords();
        if (words == null || words.isEmpty()) return;

        int wordCount = words.size();
        float lineHeight = 70; // Space between words
        float totalHeight = wordCount * lineHeight;
        float startY = centerY - totalHeight / 2 + lineHeight / 2;

        for (int i = 0; i < wordCount; i++) {
            String word = words.get(i);
            String claimer = currentWordRound.getClaimerAt(i);
            float y = startY + i * lineHeight;

            if (claimer != null) {
                // Word is claimed - draw strikethrough and claimer name
                canvas.drawText(word, centerX, y, claimedWordPaint);
                canvas.drawText("(" + claimer + ")", centerX, y + 25, claimerTextPaint);
            } else {
                // Word is available - draw in gold
                canvas.drawText(word, centerX, y, wordPaint);
            }
        }
    }
}
