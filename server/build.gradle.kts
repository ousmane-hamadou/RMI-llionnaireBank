plugins {
    application
}

group = "com.github.ousmanehamadou"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.github.ousmanehamadou.Main")
    applicationDefaultJvmArgs = listOf("-XX:+UseZGC", "--enable-preview")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.add("--enable-preview")
//    options.compilerArgs.add("-Xlint:preview")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-preview")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.picocli)
    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

}