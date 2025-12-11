package com.github.alexisvisco.demospringkotlin.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@Configuration
class S3Config {

    @Value("\${S3_ENDPOINT:#{null}}")
    private var endpoint: String? = null

    @Value("\${S3_ACCESS_KEY:#{null}}")
    private var accessKey: String? = null

    @Value("\${S3_SECRET_KEY:#{null}}")
    private var secretKey: String? = null

    @Value("\${S3_REGION:us-east-1}")
    private var region: String = "us-east-1"

    @Bean
    fun s3Client(): S3Client {
        requireNotNull(accessKey) { "S3_ACCESS_KEY is required" }
        requireNotNull(secretKey) { "S3_SECRET_KEY is required" }

        val builder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                )
            )

        // If endpoint is configured (for MinIO), use it with path-style access
        if (!endpoint.isNullOrEmpty()) {
            builder
                .endpointOverride(URI.create(endpoint))
                .forcePathStyle(true)
        }

        return builder.build()
    }

    @Bean
    fun s3Presigner(): S3Presigner {
        requireNotNull(accessKey) { "S3_ACCESS_KEY is required" }
        requireNotNull(secretKey) { "S3_SECRET_KEY is required" }

        val builder = S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                )
            )

        // If endpoint is configured (for MinIO), use it
        if (!endpoint.isNullOrEmpty()) {
            builder.endpointOverride(URI.create(endpoint))
        }

        return builder.build()
    }
}
