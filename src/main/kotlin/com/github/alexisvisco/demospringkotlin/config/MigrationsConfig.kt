package com.github.alexisvisco.demospringkotlin.config

import com.github.alexisvisco.demospringkotlin.migration.Migration
import com.github.alexisvisco.demospringkotlin.migration.SqlMigration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class MigrationsConfig {

    @Bean("migrations")
    @Primary
    fun allMigrations(
        kotlinMigrations: List<Migration>,  // Spring auto-collects @Component migrations
    ): List<Migration> = kotlinMigrations + listOf(
        SqlMigration.fromFile("db/migrations/20251211123419_init.sql"),
    )
}
