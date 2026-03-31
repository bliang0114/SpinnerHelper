import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.11.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = "cn.github.spinner"
version = "2.2.0"

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
            sinceBuild = "251"
        }
        changeNotes = """
            V2.2.0
            ---
            1. 修复Spinner文件在Spinner视图与Text视图切换编辑卡顿的问题
            2. 修复首次打开项目后，执行MQL，结果未显示的问题
            3. 新增MQL编辑器中对执行结果的状态显示
            4. 优化部分代码
    """.trimIndent()
    }
    publishing {
        token = providers.environmentVariable("ORG_GRADLE_PROJECT_intellijPlatformPublishingToken")
        version = "2.2.0"
    }
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java", "gen")
        }
        kotlin {
            srcDirs("src/main/kotlin", "gen")
        }
        resources {
            srcDir("resources")
        }
    }
}

tasks {
    // Set the JVM compatibility versions
    generateLexer {
        sourceFile.set(file("src/main/java/cn/github/spinner/editor/highlights/MQL.flex"))
        targetOutputDir.set(file("gen/cn/github/spinner/editor/highlights/"))
        purgeOldFiles = true
    }

    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.encoding = "UTF-8"
        dependsOn(generateLexer)
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
        dependsOn(generateLexer)
    }

    buildSearchableOptions {
        enabled = false  // 直接禁用，发布插件不需要可搜索选项索引
    }

    publishPlugin {
        token = providers.environmentVariable("ORG_GRADLE_PROJECT_intellijPlatformPublishingToken")
        version = "2.2.0"
    }
}
