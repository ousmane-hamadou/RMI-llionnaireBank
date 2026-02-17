plugins {
    application
}

group = "com.github.ousmanehamadou"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.github.ousmanehamadou.Main")

    applicationDefaultJvmArgs = listOf("-XX:+UseZGC")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

dependencies {
    implementation(projects.shared)
    implementation(libs.picocli)
}