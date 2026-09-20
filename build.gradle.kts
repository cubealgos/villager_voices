// Wait, they talk now? — common/fabric/neoforge module split (docs/spec/04-architecture.md
// ARCH-DEC-001, the JEI shape). The root project holds no sources of its own; each subproject is
// built and versioned independently.

allprojects {
    repositories {
        mavenCentral()
    }
}
