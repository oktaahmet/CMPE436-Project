package com.example.wrd;
import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 436;

    private final MessageType type;
    private final Object data;
    private final long timestamp;

    public Message(MessageType type, Object data) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public MessageType getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}