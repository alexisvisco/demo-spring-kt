package com.github.alexisvisco.demospringkotlin.model

import com.github.alexisvisco.demospringkotlin.utils.idgen.PrefixedUuid
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "codes")
class Code(
    @Column(name = "value")
    var value: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    var type: CodeType,

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    var ownerType: CodeOwnerType,

    @Column(name = "owner_id", nullable = false)
    var ownerId: String,

    @Column(name = "expires_at")
    var expiresAt: LocalDateTime = LocalDateTime.now().plusMinutes(15),

    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @PrefixedUuid(prefix = "c")
    @Column(name = "id", length = 50, updatable = false)
    var id: String? = null
}

object CodeUtils {
    const val CODE_LENGTH = 6

    fun generateRandomCode(size: Int = CODE_LENGTH): String {
        val allowedChars = ('0'..'9')
        return (1..size)
            .map { allowedChars.random() }
            .joinToString("")
    }
}

enum class CodeType {
    UNSPECIFIED,
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
}

enum class CodeOwnerType {
    USER,
}
