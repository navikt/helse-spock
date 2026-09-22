plugins {
    id("no.nav.helse.sas.sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.spock.opprydding_dev.AppKt"
    imageName = "${rootProject.name}-opprydding-dev"
}

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.kotliquery)
    implementation(libs.cloud.sql.postgres.socket.factory)

    testImplementation(project(":spock"))
    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
    testImplementation(libs.tbd.libs.postgres.testdatabaser)
}
