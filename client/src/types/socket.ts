import { Socket } from "socket.io-client"

type SocketId = string

enum SocketEvent {
    JOIN_REQUEST = "join-request",
    JOIN_ACCEPTED = "join-accepted",
    USER_JOINED = "user-joined",
    USER_DISCONNECTED = "user-disconnected",
    SYNC_FILE_STRUCTURE = "sync-file-structure",
    DIRECTORY_CREATED = "directory-created",
    DIRECTORY_UPDATED = "directory-updated",
    DIRECTORY_RENAMED = "directory-renamed",
    DIRECTORY_DELETED = "directory-deleted",
    FILE_CREATED = "file-created",
    FILE_UPDATED = "file-updated",
    FILE_RENAMED = "file-renamed",
    FILE_DELETED = "file-deleted",
    USER_OFFLINE = "offline",
    USER_ONLINE = "online",
    SEND_MESSAGE = "send-message",
    RECEIVE_MESSAGE = "receive-message",
    TYPING_START = "typing-start",
    TYPING_PAUSE = "typing-pause",
    USERNAME_EXISTS = "username-exists",
    REQUEST_DRAWING = "request-drawing",
    SYNC_DRAWING = "sync-drawing",
    DRAWING_UPDATE = "drawing-update",
    ROOM_SNAPSHOT = "room-snapshot",
    FILE_LOCK_REQUEST = "file-lock-request",
    FILE_LOCK_GRANTED = "file-lock-granted",
    FILE_LOCK_DENIED = "file-lock-denied",
    FILE_LOCK_RELEASE = "file-lock-release",
    FILE_LOCK_STATE = "file-lock-state",
    ROOM_NOT_FOUND = "room-not-found",
}

interface SocketContext {
    socket: Socket
}

export interface FileLockInfo {
    fileId: string
    socketId: string
    username: string
}

export { SocketEvent, SocketContext, SocketId }
