package com.github.alexisvisco.demospringkotlin.migration

import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SqlMigration(
    private val filePath: String,
) : Migration, JdbcTemplateAware {

    private lateinit var jdbcTemplate: JdbcTemplate

    private val name: String
    private val timestamp: LocalDateTime
    private val upSql: String
    private val downSql: String

    override fun setJdbcTemplate(jdbcTemplate: JdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate
    }

    init {
        val (parsedName, parsedTimestamp) = parseFileName(filePath)
        name = parsedName
        timestamp = parsedTimestamp

        val content = ClassPathResource(filePath).inputStream.bufferedReader().use { it.readText() }
        val (up, down) = parseSqlContent(content)
        upSql = up
        downSql = down
    }

    override fun up() {
        if (upSql.isBlank()) {
            throw IllegalStateException("no up SQL found for migration $name")
        }
        jdbcTemplate.execute(upSql)
    }

    override fun down() {
        if (downSql.isNotBlank()) {
            jdbcTemplate.execute(downSql)
        }
    }

    override fun version(): Pair<String, LocalDateTime> = name to timestamp

    private fun parseFileName(path: String): Pair<String, LocalDateTime> {
        val fileName = path.substringAfterLast('/')
        if (!fileName.endsWith(".sql")) {
            throw IllegalArgumentException("not a SQL file: $path")
        }

        val base = fileName.removeSuffix(".sql")
        val parts = base.split("_", limit = 2)

        if (parts.size != 2) {
            throw IllegalArgumentException("invalid file format, expected {timestamp}_{name}.sql: $path")
        }

        val timestampStr = parts[0]
        val name = parts[1]

        val timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

        return name to timestamp
    }

    private fun parseSqlContent(content: String): Pair<String, String> {
        val lines = content.lines()
        var currentSection = ""
        val upLines = mutableListOf<String>()
        val downLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()

            when {
                trimmed.startsWith("-- migrate:up", ignoreCase = true) -> {
                    currentSection = "up"
                    continue
                }
                trimmed.startsWith("-- migrate:down", ignoreCase = true) -> {
                    currentSection = "down"
                    continue
                }
            }

            when (currentSection) {
                "up" -> upLines.add(line)
                "down" -> downLines.add(line)
            }
        }

        if (upLines.isEmpty()) {
            throw IllegalArgumentException("no -- migrate:up section found")
        }

        return upLines.joinToString("\n").trim() to downLines.joinToString("\n").trim()
    }

    companion object {
        fun fromFile(filePath: String): SqlMigration {
            return SqlMigration(filePath)
        }
    }
}
