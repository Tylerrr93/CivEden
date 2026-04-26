plugins {
    alias(libs.plugins.paper.userdev)
}

version = "2.0.0"

repositories {
    maven("https://maven.maxhenkel.de/repository/public")
}

dependencies {
    paperweight {
        paperDevBundle(libs.versions.paper)
    }

    compileOnly(project(":plugins:civmodcore-paper"))
    compileOnly(project(":plugins:citadel-paper"))
    compileOnly(project(":plugins:namelayer-paper"))

    // SimpleVoiceChat — soft dependency; only loaded when the plugin is present at runtime
    compileOnly("de.maxhenkel.voicechat:voicechat-api:2.5.27")
}
