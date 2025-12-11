package com.github.alexisvisco.demospringkotlin.migration

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MigrationService(
    private var migrations: List<Migration>,
    private val tracker: MigrationTracker,
    private val jdbcTemplate: JdbcTemplate
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun migrate() {
        tracker.createTableIfNotExists()
        prepareMigrations()

        val applied = tracker.findAll()
        val pending = migrations
            .filterNot { applied.contains(formatVersion(it)) }
            .sortedBy { it.version().second }

        if (pending.isEmpty()) {
            logger.info("no pending migrations")
            return
        }

        pending.forEach { migration ->
            runMigration(migration, isUp = true)
        }

        logger.info("applied ${pending.size} migration(s)")
    }

    fun rollback(count: Int) {
        tracker.createTableIfNotExists()
        prepareMigrations()

        val applied = tracker.findAll()
        val toRollback = migrations
            .filter { applied.contains(formatVersion(it)) }
            .sortedByDescending { it.version().second }
            .take(count)

        if (toRollback.isEmpty()) {
            logger.info("no migrations to rollback")
            return
        }

        toRollback.forEach { migration ->
            runMigration(migration, isUp = false)
        }

        logger.info("rolled back ${toRollback.size} migration(s)")
    }

    fun status() {
        tracker.createTableIfNotExists()
        prepareMigrations()

        val applied = tracker.findAll()
        val allMigrations = migrations.sortedBy { it.version().second }

        allMigrations.forEach { migration ->
            val version = formatVersion(migration)
            val (name, timestamp) = migration.version()
            val timestampStr = timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val status = if (applied.contains(version)) "applied" else "pending"
            val type = migration.javaClass.simpleName

            logger.atInfo()
                .addKeyValue("migration.version", version)
                .addKeyValue("migration.name", name)
                .addKeyValue("migration.timestamp", timestampStr)
                .addKeyValue("migration.status", status)
                .addKeyValue("migration.type", type)
                .log("Migration status")
        }

        val pendingCount = allMigrations.count { !applied.contains(formatVersion(it)) }
        val appliedCount = allMigrations.count { applied.contains(formatVersion(it)) }

        logger.atInfo()
            .addKeyValue("migrations.total", allMigrations.size)
            .addKeyValue("migrations.applied", appliedCount)
            .addKeyValue("migrations.pending", pendingCount)
            .log("Migration summary")
    }

    @Transactional
    fun runMigration(migration: Migration, isUp: Boolean) {
        val version = formatVersion(migration)

        try {
            if (isUp) {
                logger.info("applying: $version")
                migration.up()
                tracker.migrate(version)
            } else {
                logger.info("rolling back: $version")
                migration.down()
                tracker.rollback(version)
            }
        } catch (e: Exception) {
            logger.error("failed to ${if (isUp) "apply" else "rollback"} $version: ${e.message}")
            throw e
        }
    }

    private fun prepareMigrations() {
        migrations.forEach { migration ->
            if (migration is JdbcTemplateAware) {
                migration.setJdbcTemplate(jdbcTemplate)
            }
        }

        migrations = migrations.sortedBy { it.version().second }
    }

    private fun formatVersion(migration: Migration): String {
        val (name, timestamp) = migration.version()
        return "${timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}_$name"
    }
}
