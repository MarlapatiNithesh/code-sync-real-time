package com.codesync.repository;

import com.codesync.entity.RoomEntity;
import com.codesync.entity.RoomFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomFileRepository extends JpaRepository<RoomFileEntity, UUID> {

    List<RoomFileEntity> findByRoom(RoomEntity room);

    Optional<RoomFileEntity> findByRoomAndFileId(RoomEntity room, String fileId);

    void deleteByRoom(RoomEntity room);
}
