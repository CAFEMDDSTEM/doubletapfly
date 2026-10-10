plugins {
    id("net.neoforged.moddev") version "2.0.140"
}

val modId: String = providers.gradleProperty("mod_id").get()
val modName: String = providers.gradleProperty("mod_name").get()
val modVersion: String = providers.gradleProperty("mod_version").get()
val modAuthors: String = providers.gradleProperty("mod_authors").get()
val modLicense: String = providers.gradleProperty("mod_license").get()
val modDescription: String = providers.gradleProperty("mod_description").get()
val minecraftVersion: String = providers.gradleProperty("minecraft_version").get()
val minecraftVersionRange: String = providers.gradleProperty("minecraft_version_range").get()
val neoforgeVersion: String = providers.gradleProperty("neoforge_version").get()
val neoforgeLoaderRange: String = providers.gradleProperty("neoforge_loader_version_range").get()

group = providers.gradleProperty("mod_group").get()
version = modVersion

base {
    archivesName.set("$modId-$minecraftVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

neoForge {
    version = neoforgeVersion

    runs {
        register("client") {
            client()
            gameDirectory = file("run/client")
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named<ProcessResources>("processResources") {
    val expandProps = mapOf(
        "version" to modVersion,
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neoforge_version" to neoforgeVersion,
        "neoforge_loader_version_range" to neoforgeLoaderRange,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_authors" to modAuthors,
        "mod_license" to modLicense,
        "mod_description" to modDescription,
    )
    inputs.properties(expandProps)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(expandProps)
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Specification-Title" to modName,
            "Specification-Version" to modVersion,
            "Specification-Vendor" to modAuthors,
            "Implementation-Title" to modName,
            "Implementation-Version" to modVersion,
            "Implementation-Vendor" to modAuthors,
            "Built-On-Minecraft" to minecraftVersion,
        )
    }
}


