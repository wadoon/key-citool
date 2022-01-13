plugins {
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

application {
    mainClassName = "test"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
    implementation("com.atlassian.commonmark:commonmark:0.14.0")
    implementation("com.atlassian.commonmark:commonmark-ext-gfm-tables:0.14.0")
    implementation("com.atlassian.commonmark:commonmark-ext-autolink:0.14.0")
    implementation("com.atlassian.commonmark:commonmark-ext-gfm-strikethrough:0.14.0")
    implementation("com.atlassian.commonmark:commonmark-ext-ins:0.14.0")
    implementation("com.atlassian.commonmark:commonmark-ext-heading-anchor:0.14.0")
//    compile group: 'org.eclipse.jgit', name: 'org.eclipse.jgit', version: '5.7.0.202003110725-r'
}