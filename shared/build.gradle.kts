plugins {
    `java-library`
}

group = "com.github.ousmanehamadou"
version = "1.0-SNAPSHOT"

dependencies {
    compileOnly(libs.lombok);
    annotationProcessor(libs.lombok);
}