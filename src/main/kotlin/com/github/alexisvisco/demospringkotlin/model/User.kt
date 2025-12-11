package com.github.alexisvisco.demospringkotlin.model

import com.github.alexisvisco.demospringkotlin.utils.idgen.PrefixedUuid
import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime

@Entity
@Table(name = "users")
@NamedEntityGraph(
    name = "User.full",
    attributeNodes = [NamedAttributeNode("attachment")]
)
class User(
    @Column(name = "password")
    var passwordHash: String,

    @Column(name = "email")
    var email: String,

    @Column(name = "access_token")
    var accessToken: String = "",

    @Column(name = "email_verified_at")
    var emailVerifiedAt: LocalDateTime? = null,

    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JoinColumn(name = "attachment_id")
    var attachment: Attachment? = null,

    @ManyToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinTable(
        name = "user_image_picture_variant_sets",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "image_variant_set_id")]
    )
    @ImageVariantPreset(preset = ImagePreset.PROFILE_PICTURE)
    var pictures: MutableList<ImageVariantSet> = mutableListOf()
) : UserDetails {
    @Id
    @PrefixedUuid(prefix = "u")
    @Column(name = "id", length = 50, updatable = false)
    var id: String? = null

    override fun getAuthorities() = emptyList<GrantedAuthority>()

    override fun getPassword() = passwordHash

    override fun getUsername() = email

    override fun isAccountNonExpired() = true

    override fun isAccountNonLocked() = true

    override fun isCredentialsNonExpired() = true

    override fun isEnabled() = emailVerifiedAt != null
}
