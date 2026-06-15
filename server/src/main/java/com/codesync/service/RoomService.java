package com.codesync.service;

import com.codesync.dto.CreateRoomRequest;
import com.codesync.dto.RoomResponse;
import com.codesync.dto.RoomSummaryDto;
import com.codesync.entity.RegisteredUser;
import com.codesync.entity.RoomEntity;
import com.codesync.repository.RegisteredUserRepository;
import com.codesync.repository.RoomRepository;
import com.codesync.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RegisteredUserRepository registeredUserRepository;
    private final RoomDataCacheService roomDataCacheService;

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "ownerRooms", allEntries = true)
    public RoomResponse createRoom(AuthenticatedUser owner, CreateRoomRequest request) {
        RegisteredUser ownerEntity = registeredUserRepository.findById(owner.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String roomCode = generateUniqueRoomCode();

        RoomEntity room = RoomEntity.builder()
                .roomCode(roomCode)
                .name(request.getName().trim())
                .owner(ownerEntity)
                .fileStructureJson(RoomDataCacheService.DEFAULT_FILE_STRUCTURE)
                .drawingDataJson(null)
                .build();

        RoomEntity savedRoom = roomRepository.save(room);
        roomDataCacheService.seedCache(savedRoom);
        return toResponse(savedRoom);
    }

    @Cacheable(value = "ownerRooms", key = "#owner.id + '_' + #page + '_' + #size")
    public List<RoomResponse> getRoomsForOwner(AuthenticatedUser owner, int page, int size) {
        RegisteredUser ownerEntity = registeredUserRepository.findById(owner.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return roomRepository.findSummaryByOwnerOrderByCreatedAtDesc(ownerEntity, pageable).stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(value = "rooms", key = "#roomCode")
    public RoomResponse getRoomByCode(String roomCode) {
        RoomEntity room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        return toResponse(room);
    }

    public boolean roomExists(String roomCode) {
        return roomRepository.existsByRoomCode(roomCode);
    }

    private String generateUniqueRoomCode() {
        String roomCode;
        do {
            roomCode = UUID.randomUUID().toString();
        } while (roomRepository.existsByRoomCode(roomCode));
        return roomCode;
    }

    private RoomResponse toResponse(RoomEntity room) {
        return RoomResponse.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .name(room.getName())
                .ownerId(room.getOwner().getId())
                .ownerUsername(room.getOwner().getUsername())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private RoomResponse toResponse(RoomSummaryDto room) {
        return RoomResponse.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .name(room.getName())
                .ownerId(room.getOwnerId())
                .ownerUsername(room.getOwnerUsername())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
