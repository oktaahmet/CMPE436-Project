package com.example.wrd;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;

public class NetworkManager {
    private static NetworkManager instance;

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private MessageListener messageListener;
    private volatile boolean connected;

    // Single thread executor for sending messages
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();

    public interface ConnectionCallback {
        void onConnected();
        void onConnectionFailed(String error);
    }

    public interface MessageListener {
        void onMessageReceived(Message message);
    }

    private NetworkManager() {
        connected = false;
    }

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public void connect(String host, int port, String username, ConnectionCallback callback) {
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                output = new ObjectOutputStream(socket.getOutputStream());
                output.flush();
                input = new ObjectInputStream(socket.getInputStream());

                connected = true;

                // Send join server message
                sendMessageInternal(new Message(MessageType.JOIN_SERVER, username));

                // Start receive thread
                startReceiveThread();

                if (callback != null) {
                    callback.onConnected();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onConnectionFailed(e.getMessage());
                }
            }
        }).start();
    }

    private void startReceiveThread() {
        Thread receiveThread = new Thread(() -> {
            while (connected) {
                try {
                    Message message = (Message) input.readObject();
                    if (messageListener != null) {
                        messageListener.onMessageReceived(message);
                    }
                } catch (EOFException e) {
                    connected = false;
                    break;
                } catch (Exception e) {
                    if (connected) {
                        e.printStackTrace();
                    }
                    connected = false;
                    break;
                }
            }
        }, "NetworkReceiveThread");
        receiveThread.start();
    }

    // Async send using executor (thread-safe, no thread explosion)
    public void sendMessage(Message message) {
        if (!connected) return;

        sendExecutor.execute(() -> sendMessageInternal(message));
    }

    // Synchronous send - blocks until message is sent
    public void sendMessageSync(Message message) {
        if (!connected) return;
        sendMessageInternal(message);
    }

    // Internal synchronized send
    private void sendMessageInternal(Message message) {
        try {
            synchronized (output) {
                output.writeObject(message);
                output.flush();
                output.reset();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public void disconnect() {
        connected = false;
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}