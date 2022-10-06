import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"
}

val junitJupiterVersion = "5.9.1"
val mainClass = "no.nav.helse.spock.AppKt"

group = "no.nav.helse"
version = properties["version"] ?: "local-build"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:2022092314391663936769.9d5d33074875")

    implementation("org.flywaydb:flyway-core:8.5.13")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("no.nav:vault-jdbc:1.3.9")
    implementation("com.github.seratch:kotliquery:1.9.0")

    testImplementation("org.testcontainers:postgresql:1.17.5")
    testImplementation("org.awaitility:awaitility:4.2.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    named<KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = "17"
    }

    withType<Jar> {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = mainClass
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("skipped", "failed")
        }
    }

    withType<Wrapper> {
        gradleVersion = "7.4.1"
    }
}
