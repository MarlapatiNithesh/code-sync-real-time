package com.codesync.model;

public enum UserConnectionStatus {
    OFFLINE("offline"),
    ONLINE("online");

    private final String value;

    UserConnectionStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
