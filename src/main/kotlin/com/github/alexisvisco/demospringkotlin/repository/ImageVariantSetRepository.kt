package com.github.alexisvisco.demospringkotlin.repository

import com.github.alexisvisco.demospringkotlin.model.ImageVariantSet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImageVariantSetRepository : JpaRepository<ImageVariantSet, String>
