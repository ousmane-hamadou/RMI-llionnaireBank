plugins {
    application
}

group = "com.github.ousmanehamadou"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.github.ousmanehamadou.Main")

    applicationDefaultJvmArgs = listOf("-XX:+UseZGC")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.picocli)

}