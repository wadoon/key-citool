plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("simplePlugin") {
            id = "io.wadoon.key-citool"
            implementationClass = "io.wadoon.citool.gradle.Plugin"
        }
    }
}
