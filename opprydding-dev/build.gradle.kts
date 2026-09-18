plugins {
    kotlin("jvm") version "2.3.0"
    application
}

repositories {
    mavenCentral()
    if (providers.environmentVariable("GITHUB_ACTIONS").orNull == "true") {
        maven {
            url = uri("https://maven.pkg.github.com/navikt/maven-release")
            credentials {
                username = "token"
                password = providers.environmentVariable("GITHUB_TOKEN").orNull!!
            }
        }
    } else {
        maven("https://repo.adeo.no/repository/github-package-registry-navikt/")
    }
}

application {
    mainClass.set("no.nav.helse.spock.opprydding_dev.AppKt")
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:2026011411051768385145.e8ebad1177b4")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("com.github.seratch:kotliquery:1.9.0")

    testImplementation(project(":spock"))
    testImplementation("com.github.navikt.tbd-libs:rapids-and-rivers-test:2026.01.22-09.16-1d3f6039")
    testImplementation("com.github.navikt.tbd-libs:postgres-testdatabaser:2026.01.22-09.16-1d3f6039")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of("21"))
    }
}

tasks {
    withType<Jar> {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.helse.spock.opprydding_dev.AppKt"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
                if (!file.exists()) it.copyTo(file)
            }
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }
}
