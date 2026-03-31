import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.11.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = "cn.github.spinner"
version = "3.0.1"

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
        bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }
        changeNotes = """
            <h2>3.0.1</h2>
            <ul>
            <li>完善 MQL 自动补全规则，`type`、`rel`、`relationship`、`temp query bus` 在双引号场景下支持同一对引号内的逗号分隔名称列表补全。</li>
            <li>修复包含空格的类型名、关系名在连续补全时被错误拆成多对引号的问题，生成结果统一为 <code>"A,B"</code> 这种 MQL 可执行格式。</li>
            <li>优化 `temp query bus`、`query connection rel`、`where` 等关键字后的上下文提示与插入行为，补全后可继续在当前语义位置稳定提示。</li>
            <li>MQL编辑器布局支持拖动调整</li>
            </ul>
            <h2>3.0.0</h2>
            <ul>
            <li>MQL 编辑器已重构为分栏视图：左侧源码、右侧执行结果，替换旧版结果工具窗口。</li>
            <li>执行结果现已保留语句与输出映射、长期历史记录、换行切换、检索功能、语法高亮输出、侧边标记及失败提示气泡。</li>
            <li>针对包含 $1、$2 等占位符的 MQL 命令，新增非阻塞式执行前弹窗录入。</li>
            <li>环境管理新增 CAS 开关（新增、编辑、复制流程均支持），适配 CAS 与非 CAS 服务器的连接、重连逻辑。</li>
            <li>优化历史环境配置兼容性，并细化多项 MQL 编辑器与执行交互体验。</li>
            </ul>
    """.trimIndent()
    }
    publishing {
        token = providers.environmentVariable("ORG_GRADLE_PROJECT_intellijPlatformPublishingToken")
        version = "3.0.1"
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
        enabled = false
    }

    publishPlugin {
        token = providers.environmentVariable("ORG_GRADLE_PROJECT_intellijPlatformPublishingToken")
        version = "3.0.1"
    }
}
