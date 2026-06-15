package com.codesync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSummaryDto {

    private UUID id;
    private String roomCode;
    private String name;
    private UUID ownerId;
    private String ownerUsername;
    private Instant createdAt;
    private Instant updatedAt;
}
