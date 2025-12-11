plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.hibernate.orm") version "7.1.8.Final"
    id("org.graalvm.buildtools.native") version "0.11.3"
    kotlin("plugin.jpa") version "2.2.21"
}

group = "com.github.alexisvisco"
version = "0.0.1-SNAPSHOT"
description = "demo-spring-kotlin"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("com.github.f4b6a3:uuid-creator:6.1.0")
    implementation("org.codehaus.janino:janino:3.1.12")
    implementation("p6spy:p6spy:3.9.1")

    implementation("org.apache.tika:tika-core:3.2.3")

    // Image processing
    implementation("com.sksamuel.scrimage:scrimage-core:4.3.5")
    implementation("com.sksamuel.scrimage:scrimage-webp:4.3.5")

    implementation("io.temporal:temporal-sdk:1.32.0")
    implementation("io.temporal:temporal-spring-boot-starter:1.31.0")

    // Add Jackson 2.x Kotlin module for Temporal (Temporal uses Jackson 2.x, not 3.x)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    // AWS S3
    implementation("software.amazon.awssdk:s3:2.29.33")

    // Hibernate JSONB support
    implementation("io.hypersistence:hypersistence-utils-hibernate-71:3.13.2")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-client-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")

    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

hibernate {
    enhancement {
        enableAssociationManagement = true
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
