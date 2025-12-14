package com.example.wrd;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LobbyListActivity extends AppCompatActivity {
    private static final long AUTO_REFRESH_INTERVAL = 500; // 0.5 seconds

    private LobbyAdapter adapter;
    private ArrayList<LobbyInfo> lobbies;
    private boolean isJoiningLobby = false;
    private NetworkManager.MessageListener messageListener;
    private Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;

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

        setContentView(R.layout.activity_lobby_list);

        ListView lobbyListView = findViewById(R.id.lobbyListView);

        lobbies = new ArrayList<>();
        adapter = new LobbyAdapter(this, lobbies);
        lobbyListView.setAdapter(adapter);

        lobbyListView.setOnItemClickListener((parent, view, position, id) -> {
            if (!isJoiningLobby) {
                joinLobby(lobbies.get(position));
            }
        });

        // Create message listener
        messageListener = message -> runOnUiThread(() -> handleMessage(message));

        // Setup auto-refresh
        autoRefreshHandler = new Handler(Looper.getMainLooper());
        autoRefreshRunnable = () -> {
            if (!isJoiningLobby) {
                loadLobbies();
            }
            autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL);
        };

        NetworkManager.getInstance().setMessageListener(messageListener);
        loadLobbies();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NetworkManager.getInstance().setMessageListener(messageListener);
        // Start auto-refresh
        autoRefreshHandler.post(autoRefreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop auto-refresh when activity is not visible
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    private void loadLobbies() {
        NetworkManager.getInstance().sendMessage(new Message(MessageType.GET_LOBBIES, null));
    }

    private void handleMessage(Message message) {
        switch (message.getType()) {
            case LOBBY_LIST:
                LobbyInfo[] lobbyArray = (LobbyInfo[]) message.getData();
                lobbies.clear();
                Collections.addAll(lobbies, lobbyArray);
                adapter.notifyDataSetChanged();
                break;

            case JOIN_LOBBY_SUCCESS:
                if (isJoiningLobby) {
                    isJoiningLobby = false;
                    String lobbyId = (String) message.getData();
                    openGameActivity(lobbyId);
                }
                break;

            case JOIN_LOBBY_FAILED:
                String error = (String) message.getData();
                Toast.makeText(this, "Failed to join: " + error, Toast.LENGTH_SHORT).show();
                isJoiningLobby = false;
                break;
        }
    }

    private void joinLobby(LobbyInfo lobby) {
        isJoiningLobby = true;
        NetworkManager.getInstance().sendMessage(new Message(MessageType.JOIN_LOBBY, lobby.getId()));
    }

    private void openGameActivity(String lobbyId) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("lobbyId", lobbyId);
        startActivity(intent);
    }

    private static class LobbyAdapter extends BaseAdapter {
        private final Context context;
        private final List<LobbyInfo> lobbyList;

        public LobbyAdapter(Context context, List<LobbyInfo> lobbyList) {
            this.context = context;
            this.lobbyList = lobbyList;
        }

        @Override
        public int getCount() {
            return lobbyList.size();
        }

        @Override
        public Object getItem(int position) {
            return lobbyList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.lobby_item, parent, false);
            }

            LobbyInfo lobby = lobbyList.get(position);

            TextView lobbyName = convertView.findViewById(R.id.lobbyName);
            TextView lobbyPlayers = convertView.findViewById(R.id.lobbyPlayers);
            TextView lobbyStatus = convertView.findViewById(R.id.lobbyStatus);

            lobbyName.setText(lobby.getName());
            lobbyPlayers.setText(String.format(Locale.US,"%d/%d players", lobby.getCurrentPlayers(), lobby.getMaxPlayers()));

            if (lobby.isGameActive()) {
                lobbyStatus.setText("● IN GAME");
                lobbyStatus.setTextColor(0xFFFF5252);
            } else {
                lobbyStatus.setText("● WAITING");
                lobbyStatus.setTextColor(0xFF4CAF50);
            }

            return convertView;
        }
    }
}