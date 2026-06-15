package com.codesync.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedRoomData {

    private String fileStructureJson;
    private String drawingDataJson;
    @Builder.Default
    private boolean dirty = false;
}
