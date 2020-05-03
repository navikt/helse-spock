val junitJupiterVersion = "5.6.0"
val mainClass = "no.nav.helse.spock.AppKt"

val githubUser: String by project
val githubPassword: String by project

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:1.47c31b4")

    implementation("org.flywaydb:flyway-core:6.3.1")
    implementation("com.zaxxer:HikariCP:3.4.2")
    implementation("no.nav:vault-jdbc:1.3.1")
    implementation("com.github.seratch:kotliquery:1.3.1")

    testImplementation("com.opentable.components:otj-pg-embedded:0.13.3")
    testImplementation("org.awaitility:awaitility:3.1.6")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/navikt/rapids-and-rivers")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}

tasks.named<Jar>("jar") {
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
