package com.codesync.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseShardingInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Checking database sharding and partitioning status...");
        try {
            // Drop foreign key constraints first to allow partitioning (sharding)
            dropForeignKeysIfExist("rooms");
            dropForeignKeysIfExist("room_files");

            // Shard tables using native MySQL key partitioning
            shardTableIfNecessary("users");
            shardTableIfNecessary("rooms");
            shardTableIfNecessary("room_files");
        } catch (Exception e) {
            log.error("Failed to apply database partitioning / sharding: {}", e.getMessage(), e);
        }
    }

    private void dropForeignKeysIfExist(String tableName) {
        try {
            String findFkSql = "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                               "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND REFERENCED_TABLE_NAME IS NOT NULL";
            List<String> fkConstraints = jdbcTemplate.queryForList(findFkSql, String.class, tableName);

            for (String fk : fkConstraints) {
                log.info("Dropping foreign key constraint '{}' from table '{}' to allow partitioning...", fk, tableName);
                jdbcTemplate.execute(String.format("ALTER TABLE %s DROP FOREIGN KEY %s", tableName, fk));
            }
        } catch (Exception e) {
            log.warn("Could not check/drop foreign keys for table '{}': {}", tableName, e.getMessage());
        }
    }

    private void shardTableIfNecessary(String tableName) {
        try {
            String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.PARTITIONS " +
                              "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND PARTITION_NAME IS NOT NULL";
            Integer partitionCount = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName);

            if (partitionCount != null && partitionCount > 0) {
                log.info("Table '{}' is already partitioned (sharded) with {} partitions.", tableName, partitionCount);
            } else {
                log.info("Table '{}' is not partitioned. Applying KEY-based partitioning (4 partitions)...", tableName);
                // Apply KEY-based partitioning on the primary key (id) which automatically shards data
                String alterSql = String.format("ALTER TABLE %s PARTITION BY KEY(id) PARTITIONS 4", tableName);
                jdbcTemplate.execute(alterSql);
                log.info("Successfully partitioned/sharded table '{}'.", tableName);
            }
        } catch (Exception e) {
            log.warn("Could not apply partitioning/sharding to table '{}'. Reason: {}", tableName, e.getMessage());
        }
    }
}
