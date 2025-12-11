package com.github.alexisvisco.demospringkotlin.repository

import com.github.alexisvisco.demospringkotlin.model.Code
import com.github.alexisvisco.demospringkotlin.model.CodeType
import com.github.alexisvisco.demospringkotlin.model.CodeOwnerType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface CodeRepository : JpaRepository<Code, String> {
    @Query("""
    SELECT c FROM Code c 
    WHERE c.type = :type 
    AND c.ownerType = :ownerType 
    AND c.value = :value 
    AND c.expiresAt > :now
""")
    fun findValidCode(
        type: CodeType,
        ownerType: CodeOwnerType,
        value: String,
        now: LocalDateTime
    ): Code?
}
