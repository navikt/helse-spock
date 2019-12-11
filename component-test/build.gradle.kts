import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
    testImplementation(project(":spock"))
    testImplementation("com.zaxxer:HikariCP:3.3.1")
    testImplementation("io.ktor:ktor-server-netty:1.2.4")
    testImplementation("com.opentable.components:otj-pg-embedded:0.13.1")
    testImplementation("org.awaitility:awaitility:3.1.6")
    testImplementation("no.nav:kafka-embedded-env:2.2.3")
}

repositories {
    maven("http://packages.confluent.io/maven/")
}
