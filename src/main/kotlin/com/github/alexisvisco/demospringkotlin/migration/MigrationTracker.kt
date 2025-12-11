package com.github.alexisvisco.demospringkotlin.migration

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class MigrationTracker(
    private val jdbcTemplate: JdbcTemplate
) {
    fun createTableIfNotExists() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS schema_migrations (
                version VARCHAR(255) PRIMARY KEY,
                applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """)
    }

    fun findAll(): Set<String> {
        return jdbcTemplate.queryForList(
            "SELECT version FROM schema_migrations",
            String::class.java
        ).toSet()
    }

    fun migrate(version: String) {
        jdbcTemplate.update(
            "INSERT INTO schema_migrations (version) VALUES (?)",
            version
        )
    }

    fun rollback(version: String) {
        jdbcTemplate.update(
            "DELETE FROM schema_migrations WHERE version = ?",
            version
        )
    }
}
