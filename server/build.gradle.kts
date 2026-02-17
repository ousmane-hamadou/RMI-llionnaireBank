plugins {
    application
}

group = "com.github.ousmanehamadou"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.github.ousmanehamadou.Main")
    applicationDefaultJvmArgs = listOf()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

dependencies {
    implementation(projects.shared)
    implementation(libs.picocli)
    implementation(libs.lombok)
    implementation(libs.resilience4j.retry)
    annotationProcessor(libs.lombok)

}