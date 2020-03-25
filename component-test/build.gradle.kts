val githubUser: String by project
val githubPassword: String by project

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    testImplementation(project(":spock"))
    testImplementation("com.github.navikt:rapids-and-rivers:1.1822995")
    testImplementation("com.zaxxer:HikariCP:3.4.2")
    testImplementation("com.github.seratch:kotliquery:1.3.1")
    testImplementation("com.opentable.components:otj-pg-embedded:0.13.3")
    testImplementation("org.awaitility:awaitility:3.1.6")
    testImplementation("no.nav:kafka-embedded-env:2.3.0")
}

repositories {
    maven("http://packages.confluent.io/maven/")
    maven {
        url = uri("https://maven.pkg.github.com/navikt/rapids-and-rivers")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}
