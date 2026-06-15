package com.codesync.repository;

import com.codesync.dto.RoomSummaryDto;
import com.codesync.entity.RegisteredUser;
import com.codesync.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<RoomEntity, UUID> {

    @Query("SELECT r FROM RoomEntity r JOIN FETCH r.owner WHERE r.roomCode = :roomCode")
    Optional<RoomEntity> findByRoomCode(@Param("roomCode") String roomCode);

    @Query("SELECT new com.codesync.dto.RoomSummaryDto(r.id, r.roomCode, r.name, r.owner.id, r.owner.username, r.createdAt, r.updatedAt) " +
           "FROM RoomEntity r WHERE r.owner = :owner ORDER BY r.createdAt DESC")
    List<RoomSummaryDto> findSummaryByOwnerOrderByCreatedAtDesc(@Param("owner") RegisteredUser owner, org.springframework.data.domain.Pageable pageable);

    boolean existsByRoomCode(String roomCode);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM RoomEntity r WHERE r.createdAt < :cutoff AND r.id NOT IN (SELECT rf.room.id FROM RoomFileEntity rf)")
    int deleteEmptyRooms(@Param("cutoff") java.time.Instant cutoff);
}
