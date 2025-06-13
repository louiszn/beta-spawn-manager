plugins {
    id("maven-publish")
    id("fabric-loom") version "1.9.2"
    id("babric-loom-extension") version "1.9.3"
    id("com.gradleup.shadow") version "8.3.6"
}

group = project.properties["maven_group"] as String;
version = project.properties["mod_version"] as String;

java.sourceCompatibility = JavaVersion.VERSION_17;
java.targetCompatibility = JavaVersion.VERSION_17;

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.glass-launcher.net/babric")
    maven("https://maven.glass-launcher.net/snapshots/")
    maven("https://maven.glass-launcher.net/releases/")
    maven("https://maven.minecraftforge.net/")
    maven("https://jitpack.io/")
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven")
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }

    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${project.properties["minecraft_version"]}")
    mappings("net.glasslauncher:biny:${project.properties["mappings_version"]}:v2")
    modImplementation("babric:fabric-loader:${project.properties["loader_version"]}")

    implementation("org.slf4j:slf4j-api:1.8.0-beta4")
    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.17.1")

    implementation("org.spongepowered:configurate-yaml:4.2.0")
    shadow("org.spongepowered:configurate-yaml:4.2.0")

    modImplementation("com.github.matthewperiut:retrocommands:0.5.7") {
        isTransitive = false
    }
}

tasks {
    shadowJar {
        relocate("org.spongepowered.configurate", "${project.properties["maven_group"]}.shadow.configurate")

        archiveClassifier.set("")
        configurations = listOf(project.configurations.getByName("shadow"))
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
    }

    build {
        dependsOn(remapJar)
    }
}

tasks.withType<ProcessResources> {
    inputs.property("version", project.properties["version"])

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.properties["version"]))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

java {
    withSourcesJar()
}

tasks.withType<Jar> {
    from("LICENSE") {
        rename { "${it}_${project.properties["archivesBaseName"]}" }
    }
}
