plugins {
    id("java")
}

group = "cn.github"
version = "1.1.0"

repositories {
    maven {
        url = uri("https://maven.aliyun.com/repository/central")
    }
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("cn.hutool:hutool-json:5.8.40")
    compileOnly(project(":matrix-driver"))
    compileOnly(fileTree("libs"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}