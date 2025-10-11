plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

group = "com.fintrack"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.kotlinx.datetime)
    implementation("io.ktor:ktor-server-auth:2.3.0")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.7")
    implementation("org.mindrot:jbcrypt:0.4") // for password hashing
    implementation("io.insert-koin:koin-core:3.4.3")
    implementation("io.insert-koin:koin-ktor:3.4.3")
    implementation("io.ktor:ktor-server-request-validation:3.3.0")
    implementation("io.ktor:ktor-server-call-logging:3.3.0")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    implementation("io.ktor:ktor-server-metrics-micrometer:3.3.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.11.5")
    implementation("io.ktor:ktor-server-rate-limit:3.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
