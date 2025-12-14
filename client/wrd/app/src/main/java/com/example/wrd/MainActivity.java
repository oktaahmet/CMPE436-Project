package com.example.wrd;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {
    // Change this if you want to test local, server may not be running.
    private static final String TARGET_IP = "64.226.98.180";
    private static final int PORT = 8888;

    private EditText usernameInput;

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

        setContentView(R.layout.activity_main);

        usernameInput = findViewById(R.id.usernameInput);
        Button connectButton = findViewById(R.id.connectButton);

        connectButton.setOnClickListener(v -> {
            hideKeyboard();
            connectToServer();
        });

        usernameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                connectToServer();
                return true;
            }
            return false;
        });
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

    private void connectToServer() {
        String username = usernameInput.getText().toString().trim();

        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
            return;
        }

        NetworkManager.getInstance().connect(TARGET_IP, PORT, username, new NetworkManager.ConnectionCallback() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
                    openLobbyList();
                });
            }

            @Override
            public void onConnectionFailed(final String error) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection failed: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void openLobbyList() {
        Intent intent = new Intent(this, LobbyListActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NetworkManager.getInstance().disconnect();
    }
}
