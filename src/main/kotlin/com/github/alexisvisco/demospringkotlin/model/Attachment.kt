package com.github.alexisvisco.demospringkotlin.model

import com.github.alexisvisco.demospringkotlin.utils.idgen.PrefixedUuid
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "attachments")
class Attachment(
    @Id
    @PrefixedUuid(prefix = "att")
    @Column(name = "id", length = 50, updatable = false)
    var id: String? = null,

    @Column(name = "filename", nullable = false)
    var filename: String,

    @Column(name = "content_type", nullable = false)
    var contentType: String,

    @Column(name = "byte_size", nullable = false)
    var byteSize: Long,

    @Column(name = "key", nullable = false)
    var key: String,

    @Column(name = "checksum", nullable = false)
    var checksum: String,

    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)

@Entity
@Table(
    name = "image_variants",
    indexes = [
        Index(name = "idx_image_variant_set_name_unique", columnList = "image_variant_set_id,name", unique = true)
    ]
)
class ImageVariant(
    @Id
    @PrefixedUuid(prefix = "iv")
    @Column(name = "id", length = 50, updatable = false)
    var id: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id", nullable = false)
    var attachment: Attachment,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_variant_set_id", nullable = false)
    var imageVariantSet: ImageVariantSet,

    @Column(name = "name", nullable = false)
    var name: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    var metadata: Map<String, Any>? = null,

    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)


@Entity
@Table(name = "image_variant_sets")
class ImageVariantSet(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_attachment_id", nullable = false)
    var originalAttachment: Attachment,

    @Id
    @PrefixedUuid(prefix = "ivs")
    @Column(name = "id", length = 50, updatable = false)
    var id: String? = null,

    @OneToMany(
        mappedBy = "imageVariantSet",
        fetch = FetchType.EAGER,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    var variants: MutableList<ImageVariant> = mutableListOf(),

    @Column(name = "kind")
    var kind: String? = null,

    @Column(name = "kind_id")
    var kindId: String? = null,

    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
