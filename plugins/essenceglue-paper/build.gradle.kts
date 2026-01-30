plugins {
    alias(libs.plugins.paper.userdev)
}

version = "2.0.0-SNAPSHOT"

repositories {
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    paperweight {
        paperDevBundle(libs.versions.paper)
    }

    compileOnly(project(":plugins:civmodcore-paper"))
    compileOnly(project(":plugins:banstick-paper"))
    compileOnly(project(":plugins:exilepearl-paper"))
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT")

    compileOnly(libs.bundles.nuvotifier)
}
