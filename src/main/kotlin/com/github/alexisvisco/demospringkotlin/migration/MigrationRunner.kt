package com.github.alexisvisco.demospringkotlin.migration

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
class MigrationRunner(
    private val migrationService: MigrationService,
    private val migrationGenerator: MigrationGenerator
) : ApplicationRunner {

    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val nonOptionArgs = args.nonOptionArgs

        if (nonOptionArgs.isEmpty()) {
            return // pas d'args = démarrage normal
        }

        try {
            when (nonOptionArgs[0]) {
                "db" -> handleDbCommand(nonOptionArgs.drop(1), args)
                else -> {
                    logger.error("unknown command: ${nonOptionArgs[0]}")
                    printUsage()
                    exitProcess(1)
                }
            }
        } catch (e: Exception) {
            logger.error("command failed: ${e.message}", e)
            exitProcess(1)
        }

        exitProcess(0)
    }

    private fun handleDbCommand(subcommands: List<String>, args: ApplicationArguments) {
        if (subcommands.isEmpty()) {
            logger.error("missing db subcommand")
            printUsage()
            exitProcess(1)
        }

        when (subcommands[0]) {
            "migrate" -> migrationService.migrate()

            "rollback" -> {
                val count = args.getOptionValues("count")?.firstOrNull()?.toIntOrNull() ?: run {
                    logger.error("missing or invalid --count parameter")
                    exitProcess(1)
                }
                migrationService.rollback(count)
            }

            "status" -> migrationService.status()

            "generate" -> {
                if (subcommands.size < 2) {
                    logger.error("missing migration name")
                    exitProcess(1)
                }

                val name = subcommands[1]
                val type = when {
                    args.containsOption("kt") -> MigrationType.KOTLIN
                    args.containsOption("sql") -> MigrationType.SQL
                    else -> MigrationType.SQL
                }

                migrationGenerator.generate(name, type)
            }

            else -> {
                logger.error("unknown db subcommand: ${subcommands[0]}")
                printUsage()
                exitProcess(1)
            }
        }
    }

    private fun printUsage() {
        println(
            """
            Usage:
              ./gradlew bootRun --args="db migrate"
              ./gradlew bootRun --args="db rollback --count=X"
              ./gradlew bootRun --args="db status"
              ./gradlew bootRun --args="db generate <name> [--sql|--kt]"

            Examples:
              ./gradlew bootRun --args="db generate create_users --sql"
              ./gradlew bootRun --args="db generate update_schema --kt"
        """.trimIndent()
        )
    }
}
