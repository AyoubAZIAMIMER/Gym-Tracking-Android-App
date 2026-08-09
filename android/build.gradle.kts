// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}

// This project lives on an iCloud-synced Desktop. iCloud races the build and leaves
// "<name> 2.dex" / "<name> 2.xml" conflict copies inside build/, which then fail the build with
// "Type ... is defined multiple times" or "Failed file name validation" — roughly every other
// incremental build. Build outputs are disposable and machine-local, so they belong outside the
// synced tree entirely. Override with -PforgedBuildDir=... if you need them back in-tree.
val outOfTreeBuildRoot: String = (findProperty("forgedBuildDir") as String?)
    ?: "${System.getProperty("user.home")}/.gradle-build/forged"

allprojects {
    layout.buildDirectory.set(file("$outOfTreeBuildRoot/${rootProject.name}/${project.name}"))
}
