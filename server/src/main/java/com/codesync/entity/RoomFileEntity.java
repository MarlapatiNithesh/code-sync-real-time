package com.codesync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
    name = "room_files",
    indexes = {
        @Index(name = "idx_room_files_room_id", columnList = "room_id"),
        @Index(name = "idx_room_files_room_file", columnList = "room_id, file_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "room_id",
        nullable = false,
        foreignKey = @jakarta.persistence.ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT)
    )
    private RoomEntity room;

    @Column(name = "file_id", nullable = false, length = 255)
    private String fileId;

    @Lob
    @Column(name = "compressed_content", columnDefinition = "LONGBLOB", nullable = false)
    private byte[] compressedContent;
}
