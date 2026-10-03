plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "net.dosaki"
version = (findProperty("releaseVersion") as String?) ?: "0.0.0-dev"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/")
}

val paperApi = "io.papermc.paper:paper-api:26.3.build.143-beta"

dependencies {
    compileOnly(paperApi)
    compileOnly("net.momirealms:craft-engine-core:26.9.1")
    compileOnly("net.momirealms:craft-engine-bukkit:26.9.1")

    testImplementation(paperApi)
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("paper-plugin.yml") { expand(props) }
}

tasks.jar {
    archiveBaseName.set("onelife")
}

tasks.runServer {
    minecraftVersion("26.3")
    downloadPlugins {
        modrinth("craftengine", "26.9.2")
    }
}
