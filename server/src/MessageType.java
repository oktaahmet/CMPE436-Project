package com.example.wrd;

import java.io.Serializable;

public enum MessageType implements Serializable {
    // Client to Server
    JOIN_SERVER,
    GET_LOBBIES,
    JOIN_LOBBY,
    LEAVE_LOBBY,
    PLAYER_READY,
    TYPING_UPDATE,
    SUBMIT_ANSWER,
    REQUEST_PLAYER_LIST,

    // Server to Client
    JOIN_SERVER_RESPONSE,
    LOBBY_LIST,
    JOIN_LOBBY_SUCCESS,
    JOIN_LOBBY_FAILED,
    LEAVE_LOBBY_SUCCESS,
    PLAYER_LIST_UPDATE,
    GAME_STARTING,
    GAME_STARTED,
    GAME_ENDED,
    NEW_WORD,
    REST_PERIOD,
    SCORE_UPDATE,
    PLAYER_ELIMINATED,
    WORD_CLAIMED
}
