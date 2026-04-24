plugins {
    alias(libs.plugins.paper.userdev)
}

version = "2.0.0"

dependencies {
    paperweight {
        paperDevBundle(libs.versions.paper)
    }

    compileOnly(project(":plugins:civmodcore-paper"))
    compileOnly(project(":plugins:citadel-paper"))
    compileOnly(project(":plugins:namelayer-paper"))
}
