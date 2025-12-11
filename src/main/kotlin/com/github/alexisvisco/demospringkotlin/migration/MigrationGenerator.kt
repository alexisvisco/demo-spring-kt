package com.github.alexisvisco.demospringkotlin.migration

import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Locale.getDefault

@Component
class MigrationGenerator {

    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass)
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    fun generate(name: String, type: MigrationType) {
        val timestamp = LocalDateTime.now().format(timestampFormatter)
        val fileName = "${timestamp}_${name}"

        when (type) {
            MigrationType.SQL -> {
                generateSqlFile(fileName, name, timestamp)
                updateMigrationsConfig(fileName, type)
            }

            MigrationType.KOTLIN -> {
                generateKotlinFile(fileName, name, timestamp)
                logger.info("Kotlin migration will be auto-discovered by Spring. No manual registration needed.")
            }
        }
    }

    private fun generateSqlFile(fileName: String, name: String, timestamp: String) {
        val filePath = "src/main/resources/db/migrations/${fileName}.sql"
        val file = File(filePath)

        file.parentFile.mkdirs()

        val content = """
            -- migrate:up


            -- migrate:down

        """.trimIndent()

        file.writeText(content)

        logger.info("created SQL migration: $filePath")
    }

    private fun generateKotlinFile(fileName: String, name: String, timestamp: String) {
        val className =
            "Migration${timestamp}${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }}"
        val filePath = "src/main/kotlin/com/github/alexisvisco/demospringkotlin/migration/migrations/${className}.kt"
        val file = File(filePath)

        file.parentFile.mkdirs()

        val timestampParsed = LocalDateTime.parse(timestamp, timestampFormatter)

        val content = """
            package com.github.alexisvisco.demospringkotlin.migration.migrations

            import com.github.alexisvisco.demospringkotlin.migration.Migration
            import org.springframework.jdbc.core.JdbcTemplate
            import org.springframework.stereotype.Component
            import java.time.LocalDateTime

            @Component
            class $className(
                private val jdbcTemplate: JdbcTemplate
            ) : Migration {

                override fun up() {
                    // TODO: implement migration
                }

                override fun down() {
                    // TODO: implement rollback
                }

                override fun version(): Pair<String, LocalDateTime> {
                    return "${name}" to LocalDateTime.of(
                        ${timestampParsed.year},
                        ${timestampParsed.monthValue},
                        ${timestampParsed.dayOfMonth},
                        ${timestampParsed.hour},
                        ${timestampParsed.minute},
                        ${timestampParsed.second}
                    )
                }
            }
        """.trimIndent()

        file.writeText(content)

        logger.info("created Kotlin migration: $filePath")
    }

    private fun updateMigrationsConfig(fileName: String, type: MigrationType) {
        val configPath = "src/main/kotlin/com/github/alexisvisco/demospringkotlin/config/MigrationsConfig.kt"
        val configFile = File(configPath)

        if (!configFile.exists()) {
            logger.error("MigrationsConfig.kt not found at $configPath")
            return
        }

        val content = configFile.readText()

        val newLine = "        SqlMigration.fromFile(\"db/migrations/${fileName}.sql\"),"

        val pattern = """(fun allMigrations\([^)]*\): List<Migration> = kotlinMigrations \+ listOf\()""".toRegex()
        val updatedContent = content.replace(pattern, "$1\n$newLine")

        configFile.writeText(updatedContent)

        logger.info("updated MigrationsConfig.kt with new SQL migration")
    }
}

enum class MigrationType {
    SQL, KOTLIN
}
