plugins {
    application
    antlr
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.openjfx.javafxplugin") version "0.0.10"
}


dependencies {
    implementation("org.key_project:key.core:2.10.0")
    implementation("org.key_project:key.util:2.10.0")
    implementation("com.miglayout:miglayout-javafx:11.0")
    implementation("org.fxmisc.richtext:richtextfx:0.10.9")
    implementation("org.kordamp.ikonli:ikonli-antdesignicons-pack:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-fontawesome5-pack:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    compileOnly("org.projectlombok:lombok:1.18.22")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("org.antlr:antlr4-runtime:4.9.3")
    antlr("org.antlr:antlr4:4.9.3")

    testImplementation("com.google.truth:truth:1.1.3")
}


javafx {
    version = "17"
    modules = listOf("javafx.web", "javafx.controls", "javafx.fxml", "javafx.swing")
}

application {
    mainClass.set("org.key_project.ide.KeyIde")
}

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://dl.bintray.com/kordamp/maven")
}

//https://github.com/gradle/gradle/issues/2565
/*generateGrammarSource {
    outputs.cacheIf { true }
    outputDirectory = file("build/generated-src/antlr/main/")
    sourceSets.main.java.srcDirs += outputDirectory
    val PARSER_PACKAGE_NAME = "org.key_project.ide.parser"
    arguments += ["-visitor", "-no-listener", "-package", PARSER_PACKAGE_NAME]

    doLast {
        val parserFilePattern = '*.java'
        val outputPath = generateGrammarSource.outputDirectory.canonicalPath
        val parserPackagePath = "${outputPath}/${PARSER_PACKAGE_NAME.replace('.', '/')}"
        file(parserPackagePath).mkdirs()
        copy {
            from(outputPath)
            into(parserPackagePath)
            include(parserFilePattern)
        }
        delete fileTree(outputPath) {
            include(parserFilePattern)
        }
    }
}
*/