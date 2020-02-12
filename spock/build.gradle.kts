val slf4jVersion = "1.7.25"
val ktorVersion = "1.2.4"
val jacksonVersion = "2.9.8"
val kafkaVersion = "2.3.0"
val prometheusVersion = "0.6.0"
val micrometerRegistryPrometheusVersion = "1.1.5"
val junitJupiterVersion = "5.4.0"
val flywayVersion = "6.0.0-beta"
val hikariVersion = "3.3.1"
val vaultJdbcVersion = "1.3.1"
val kotliqueryVersion = "1.3.0"

val mainClass = "no.nav.helse.spock.AppKt"

dependencies {
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("net.logstash.logback:logstash-logback-encoder:5.2")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")

    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryPrometheusVersion")
}

repositories {
    maven("https://dl.bintray.com/kotlin/ktor")
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
