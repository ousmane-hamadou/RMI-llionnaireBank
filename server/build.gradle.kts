plugins {
    `java-library`
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
    implementation(libs.resilience4j.retry)
    implementation(libs.log4j.core)
    implementation(libs.log4j.api)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

}