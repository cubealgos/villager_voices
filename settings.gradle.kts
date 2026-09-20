pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "villager_voices"

// common/fabric/neoforge module split (docs/spec/04-architecture.md ARCH-DEC-001, the JEI shape).
// The alpha ships Fabric 26.2 only (ARCH-DEC-002, DEC-005); the neoforge module and the Stonecutter
// version axis for 1.21.1 are additive fast-follows, not included here yet.
include("common")
include("fabric")
