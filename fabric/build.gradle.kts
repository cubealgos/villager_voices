// fabric — the alpha's one shipped combination (docs/spec/decisions/DEC-005-alpha-scope.md,
// docs/spec/contracts/platform-matrix.md: Fabric 26.2). Loom, Fabric API and every Minecraft
// import live only here and in neoforge, never in common (ARCH-DEC-001).
plugins {
    java
    alias(libs.plugins.loom)
}

group = "villager_voices"
version = "0.1.0+26.2"

repositories {
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.fabricLoader)
    implementation(libs.fabricApi)
    implementation(project(":common"))
    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitLauncher)
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.test {
    useJUnitPlatform()
}

loom {
    mods {
        create("villager_voices") {
            sourceSet(sourceSets.main.get())
        }
    }
}

// Server-side game tests under src/gametest (docs/spec/operations/testing.md).
fabricApi {
    configureTests {
        createSourceSet = true
        modId = "villager_voices_gametest"
        enableGameTests = true
        enableClientGameTests = false
        eula = true
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") { expand("version" to project.version) }
}

// Every game-test run starts from a fresh world (a lesson from create_civilization).
tasks.named("runGameTest") {
    doFirst { delete(layout.buildDirectory.dir("run/gameTest/world")) }
}
