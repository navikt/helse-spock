plugins {
    id("no.nav.helse.sas.sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.spock.AppKt"
}

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.kotliquery)
    implementation(libs.sykepengerLibs.logging)

    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
    testImplementation(libs.tbd.libs.postgres.testdatabaser)
}
