plugins {
    `java-library`
    `java-gradle-plugin`
    kotlin("jvm")
}

gradlePlugin {
    plugins {
        create("key-citool") {
            id = "io.github.wadoon.keycitool"
            implementationClass = "io.github.wadoon.keycitool.Plugin"
        }
    }
}

repositories {
    mavenCentral()
}
