import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    kotlin("plugin.jpa") version "2.1.0"
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.carshare"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

repositories {
    mavenCentral()
}

// Force byte-buddy to a version that supports Java 24
configurations.all {
    resolutionStrategy {
        force("net.bytebuddy:byte-buddy:1.15.11")
        force("net.bytebuddy:byte-buddy-agent:1.15.11")
    }
}

// Separate resolvable configuration to locate byte-buddy-agent jar at build time
val byteBuddyAgent: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.flywaydb:flyway-core")
    implementation("com.stripe:stripe-java:24.3.0")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.1.0")
    // byte-buddy-agent must be on the test classpath AND loaded as a javaagent
    testImplementation("net.bytebuddy:byte-buddy-agent:1.15.11")
    byteBuddyAgent("net.bytebuddy:byte-buddy-agent:1.15.11") { isTransitive = false }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // byte-buddy-agent is the correct javaagent jar (has Premain-Class in its manifest)
    jvmArgs(
        "-javaagent:${byteBuddyAgent.asPath}",
        "-Dnet.bytebuddy.experimental=true",
        "-XX:+EnableDynamicAgentLoading",
        "-Xshare:off"
    )
    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = false
    }
}
