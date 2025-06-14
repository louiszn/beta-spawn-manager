plugins {
    id("maven-publish")
    id("fabric-loom") version "1.9.2"
    id("babric-loom-extension") version "1.9.3"
    id("com.gradleup.shadow") version "8.3.6"
}

base.archivesName = project.properties["archives_base_name"] as String
group = project.properties["maven_group"] as String;
version = project.properties["mod_version"] as String;

java.sourceCompatibility = JavaVersion.VERSION_17;
java.targetCompatibility = JavaVersion.VERSION_17;

loom {
    runs {
        register("testClient") {
            source("test")
            client()
            configurations.transitiveImplementation
        }
        register("testServer") {
            source("test")
            server()
            configurations.transitiveImplementation
        }
    }
}

repositories {
    maven("https://maven.glass-launcher.net/snapshots/")
    maven("https://maven.glass-launcher.net/releases/")
    maven("https://maven.glass-launcher.net/babric")
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
}

dependencies {
    minecraft("com.mojang:minecraft:b1.7.3")
    mappings("net.glasslauncher:biny:${project.properties["mappings_version"]}:v2")
    modImplementation("babric:fabric-loader:${project.properties["loader_version"]}")

    implementation("org.apache.logging.log4j:log4j-core:2.17.2")

    implementation("org.slf4j:slf4j-api:1.8.0-beta4")
    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.17.1")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    implementation("org.jetbrains:annotations:23.0.0")
    implementation("com.google.guava:guava:33.2.1-jre")

    implementation("org.spongepowered:configurate-yaml:4.2.0")
    shadow("org.spongepowered:configurate-yaml:4.2.0")

    modImplementation("com.github.matthewperiut:retrocommands:0.5.7") {
        isTransitive = false
    }

    modImplementation("net.modificationstation:StationAPI:${project.properties["stationapi_version"]}")
    modImplementation("net.glasslauncher.mods:GlassConfigAPI:${project.properties["gcapi_version"]}")
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

java {
    withSourcesJar()
}

tasks.withType<Jar> {
    from("LICENSE") {
        rename { "${it}_${project.properties["archivesBaseName"]}" }
    }
}
