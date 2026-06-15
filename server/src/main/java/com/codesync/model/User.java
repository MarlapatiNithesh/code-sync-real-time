package com.codesync.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String username;
    private String roomId;
    private UserConnectionStatus status;
    private int cursorPosition;
    private boolean typing;
    private String currentFile;
    private String socketId;
}
