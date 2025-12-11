package com.github.alexisvisco.demospringkotlin.migration

import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

interface Migration {
    fun up()
    fun down()
    fun version(): Pair<String, LocalDateTime>
}

interface JdbcTemplateAware {
    fun setJdbcTemplate(jdbcTemplate: JdbcTemplate)
}
