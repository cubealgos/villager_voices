// common — pure Java, zero Minecraft/Fabric/NeoForge imports, ever (docs/spec/04-architecture.md
// ARCH-DEC-001). No loom/moddev plugin is applied here, so this module cannot compile against a
// loader class even by accident: there is nothing on its classpath to import.
plugins {
    java
}

group = "villager_voices"
version = "0.1.0"

dependencies {
    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitLauncher)
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.test {
    useJUnitPlatform()
}

// Enforced by the build, not just a package-purity convention (docs/spec/04-architecture.md
// ARCH-DEC-001: "a real module boundary, enforced by the build rather than a package-purity check
// alone"). Any Minecraft, Fabric or NeoForge import anywhere in this module fails the build.
val verifyLoaderFree by tasks.registering {
    group = "verification"
    description = "Fails when common imports Minecraft, Fabric or NeoForge classes."
    val sources = layout.projectDirectory.dir("src")
    inputs.dir(sources)
    doLast {
        val bad = sources.asFileTree.filter { it.extension == "java" }.files.flatMap { f ->
            f.readLines().filter { l ->
                l.startsWith("import net.minecraft") ||
                    l.startsWith("import net.fabricmc") ||
                    l.startsWith("import net.neoforged")
            }.map { l -> "${f.name}: $l" }
        }
        if (bad.isNotEmpty()) throw GradleException("common imports the game/a loader:\n" + bad.joinToString("\n"))
    }
}
tasks.named("check") { dependsOn(verifyLoaderFree) }
