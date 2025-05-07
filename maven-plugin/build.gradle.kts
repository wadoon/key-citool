plugins {
    `java`
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.apache.maven:maven-plugin-api:3.9.1")
    compileOnly("org.apache.maven.plugin-tools:maven-plugin-annotations:3.4")
}