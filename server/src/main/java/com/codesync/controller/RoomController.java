package com.codesync.controller;

import com.codesync.dto.CreateRoomRequest;
import com.codesync.dto.RoomResponse;
import com.codesync.security.AuthenticatedUser;
import com.codesync.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateRoomRequest request
    ) {
        return roomService.createRoom(user, request);
    }

    @GetMapping
    public List<RoomResponse> myRooms(
            @AuthenticationPrincipal AuthenticatedUser user,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size
    ) {
        return roomService.getRoomsForOwner(user, page, size);
    }

    @GetMapping("/code/{roomCode}")
    public RoomResponse getRoomByCode(@PathVariable String roomCode) {
        return roomService.getRoomByCode(roomCode);
    }

    @PostMapping("/code/{roomCode}/snapshot")
    @ResponseStatus(HttpStatus.OK)
    public void saveSnapshot(
            @PathVariable String roomCode,
            @RequestBody java.util.Map<String, Object> payload
    ) {
        roomService.saveSnapshot(roomCode, payload.get("fileStructure"), payload.get("drawingData"));
    }
}
