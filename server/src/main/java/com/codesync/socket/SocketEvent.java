package com.codesync.socket;

public final class SocketEvent {

    private SocketEvent() {
    }

    public static final String JOIN_REQUEST = "join-request";
    public static final String JOIN_ACCEPTED = "join-accepted";
    public static final String USER_JOINED = "user-joined";
    public static final String USER_DISCONNECTED = "user-disconnected";
    public static final String SYNC_FILE_STRUCTURE = "sync-file-structure";
    public static final String DIRECTORY_CREATED = "directory-created";
    public static final String DIRECTORY_UPDATED = "directory-updated";
    public static final String DIRECTORY_RENAMED = "directory-renamed";
    public static final String DIRECTORY_DELETED = "directory-deleted";
    public static final String FILE_CREATED = "file-created";
    public static final String FILE_UPDATED = "file-updated";
    public static final String FILE_RENAMED = "file-renamed";
    public static final String FILE_DELETED = "file-deleted";
    public static final String USER_OFFLINE = "offline";
    public static final String USER_ONLINE = "online";
    public static final String SEND_MESSAGE = "send-message";
    public static final String RECEIVE_MESSAGE = "receive-message";
    public static final String TYPING_START = "typing-start";
    public static final String TYPING_PAUSE = "typing-pause";
    public static final String USERNAME_EXISTS = "username-exists";
    public static final String REQUEST_DRAWING = "request-drawing";
    public static final String SYNC_DRAWING = "sync-drawing";
    public static final String DRAWING_UPDATE = "drawing-update";
    public static final String ROOM_SNAPSHOT = "room-snapshot";
    public static final String FILE_LOCK_REQUEST = "file-lock-request";
    public static final String FILE_LOCK_GRANTED = "file-lock-granted";
    public static final String FILE_LOCK_DENIED = "file-lock-denied";
    public static final String FILE_LOCK_RELEASE = "file-lock-release";
    public static final String FILE_LOCK_STATE = "file-lock-state";
    public static final String ROOM_NOT_FOUND = "room-not-found";
}
