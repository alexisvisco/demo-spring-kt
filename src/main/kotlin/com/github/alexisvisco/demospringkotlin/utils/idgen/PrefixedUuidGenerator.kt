package com.github.alexisvisco.demospringkotlin.utils.idgen

import com.github.alexisvisco.demospringkotlin.model.Attachment
import com.github.f4b6a3.uuid.UuidCreator
import org.hibernate.annotations.IdGeneratorType
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.generator.BeforeExecutionGenerator
import org.hibernate.generator.EventType
import org.hibernate.generator.GeneratorCreationContext
import java.lang.reflect.Member
import java.util.*
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

class PrefixedUuidGenerator(
    private val config: PrefixedUuid,
    private val member: Member,
    private val context: GeneratorCreationContext
) : BeforeExecutionGenerator {
    override fun generate(
        session: SharedSessionContractImplementor,
        owner: Any,
        currentValue: Any?,
        eventType: EventType
    ): Any {
        // Only generate if no value exists
        if (currentValue != null && currentValue is String && currentValue.isNotBlank()) {
            return currentValue
        }

        val uuidV7 = UuidCreator.getTimeOrderedEpoch()
        return "${config.prefix}_${uuidV7}"
    }

    override fun getEventTypes(): EnumSet<EventType> {
        return EnumSet.of(EventType.INSERT)
    }
}

@IdGeneratorType(PrefixedUuidGenerator::class)
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PrefixedUuid(val prefix: String)
