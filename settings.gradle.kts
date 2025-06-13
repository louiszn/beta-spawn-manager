pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "Babric"
            url = uri("https://maven.glass-launcher.net/babric")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}