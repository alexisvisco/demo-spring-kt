package com.github.alexisvisco.demospringkotlin.repository

import com.github.alexisvisco.demospringkotlin.model.Attachment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AttachmentRepository : JpaRepository<Attachment, String>
