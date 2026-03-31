import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.11.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = "cn.github.spinner"
version = "3.0.0"

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
            <h2>3.0.0</h2>
            <ul>
            <li>MQL编辑器已重构为分栏视图：左侧源码、右侧执行结果，替换旧版结果工具窗口。</li>
            <li>执行结果现已保留语句与输出映射、长期历史记录、换行切换、检索功能、语法高亮输出、侧边标记及失败提示气泡。</li>
            <li>针对包含 $1、$2 等占位符的MQL命令，新增非阻塞式执行前弹窗录入。</li>
            <li>环境管理新增CAS开关（新增/编辑/复制流程均支持），适配CAS与非CAS服务器的连接、重连逻辑。</li>
            <li>优化了历史环境配置兼容性，并精细化优化多项MQL编辑器与执行交互体验。</li>
            </ul>
    """.trimIndent()
    }
    publishing {
        token = providers.environmentVariable("ORG_GRADLE_PROJECT_intellijPlatformPublishingToken")
        version = "3.0.0"
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
        enabled = false  // disabled because publishing this plugin does not need searchable options
    }

    publishPlugin {
        token = providers.environmentVariable("ORG_GRADLE_PROJECT_intellijPlatformPublishingToken")
        version = "3.0.0"
    }
}
