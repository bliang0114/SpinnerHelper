plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = "com.bol"
version = "V2.01"

repositories {
//    mavenCentral()
    maven {
        url = uri("https://maven.aliyun.com/repository/central")
    }
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    implementation(project(":matrix-driver"))
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("cn.hutool:hutool-json:5.8.40")

    implementation("com.fifesoft:rsyntaxtextarea:3.6.0")
    implementation("net.sourceforge.plantuml:plantuml:1.2023.10")

    intellijPlatform {
        create("IC", "2025.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "241"
        }
        changeNotes = """
      Initial version
    """.trimIndent()
    }
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java", "gen")
        }
        resources {
            srcDir("resources")
            exclude("**/*.java")
        }
    }
}

tasks {
    // Set the JVM compatibility versions
    generateLexer {
        sourceFile.set(file("src/main/java/com/bol/spinner/editor/highlights/MQL.flex"))
        targetOutputDir.set(file("gen/com/bol/spinner/editor/highlights/"))
        purgeOldFiles = true
    }

    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.encoding = "UTF-8"
        dependsOn(generateLexer)
    }
}
