/*
 * Copyright 2021 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.time.Instant

///////////////////////////////
//////    PUBLISH TO SONATYPE / MAVEN CENTRAL
////// TESTING : (to local maven repo) <'publish and release' - 'publishToMavenLocal'>
////// RELEASE : (to sonatype/maven central), <'publish and release' - 'publishToSonatypeAndRelease'>
///////////////////////////////

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS   // always show the stacktrace!
gradle.startParameter.warningMode = WarningMode.All

plugins {
    java

    id("com.dorkbox.GradleUtils") version "1.12"
    id("com.dorkbox.Licensing") version "2.5.4"
    id("com.dorkbox.VersionUpdate") version "2.1"
    id("com.dorkbox.GradlePublish") version "1.9.1"
}

object Extras {
    // set for the project
    const val description = "Swt and JavaFx Utilities"
    const val group = "com.dorkbox"
    const val version = "1.0"

    // set as project.ext
    const val name = "SwtJavaFx"
    const val id = "SwtJavaFx"
    const val vendor = "Dorkbox LLC"
    const val vendorUrl = "https://dorkbox.com"
    const val url = "https://git.dorkbox.com/dorkbox/SwtJavaFx"

    val buildDate = Instant.now().toString()
}

///////////////////////////////
/////  assign 'Extras'
///////////////////////////////
GradleUtils.load("$projectDir/../../gradle.properties", Extras)
GradleUtils.fixIntellijPaths()
GradleUtils.defaultResolutionStrategy()
GradleUtils.compileConfiguration(JavaVersion.VERSION_1_8)

licensing {
    license(License.APACHE_2) {
        description(Extras.description)
        author(Extras.vendor)
        url(Extras.url)
    }
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))

            // want to include java files for the source. 'setSrcDirs' resets includes...
            include("**/*.java")
        }
    }
}

repositories {
    mavenLocal() // this must be first!
    jcenter()
}

tasks.jar.get().apply {
    manifest {
        // https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html
        attributes["Name"] = Extras.name

        attributes["Specification-Title"] = Extras.name
        attributes["Specification-Version"] = Extras.version
        attributes["Specification-Vendor"] = Extras.vendor

        attributes["Implementation-Title"] = "${Extras.group}.${Extras.id}"
        attributes["Implementation-Version"] = Extras.buildDate
        attributes["Implementation-Vendor"] = Extras.vendor

        attributes["Automatic-Module-Name"] = Extras.id
    }
}

dependencies {
    implementation("org.slf4j:slf4j-api:1.7.30")

    //  because the eclipse release of SWT is sPecIaL! Version numbers that make sense... Whaaaaaaat?
    compileOnly(GradleUtils.getSwtMavenId("3.114.100")) {
        isTransitive = false
    }

    // NOTE: we must have GRADLE ITSELF using the Oracle 1.8 JDK (which includes JavaFX).
    //       OR we will manually include JavaFx11 (which JFX8, for what we use, is compatible)

    // Java 8 include JavaFX separately. Newer versions of java bundle it (or, you can download/install it separately)
    if (JavaVersion.current() == JavaVersion.VERSION_1_8) {
        // Paths for the various executables in the Java 'bin' directory
        val javaHome = "D:/Code/extras/jdk1.8.0_181-oracle"

        val javaFxFile = File("$javaHome/jre/lib/ext/jfxrt.jar")
        println("\tJavaFX: $javaFxFile")

        if (javaFxFile.exists()) {
            compileOnly(files(javaFxFile))
        } else {
            println("\tJavaFX not found, unable to add dependency!")
        }
    } else {
        // also see: https://stackoverflow.com/questions/52569724/javafx-11-create-a-jar-file-with-gradle
        val currentOS = org.gradle.internal.os.OperatingSystem.current()
        val platform = when {
            currentOS.isWindows -> { "win" }
            currentOS.isLinux -> { "linux" }
            currentOS.isMacOsX -> { "mac" }
            else -> { "unknown" }
        }

        compileOnly("org.openjfx:javafx-base:11:${platform}")
        compileOnly("org.openjfx:javafx-graphics:11:${platform}")
        compileOnly("org.openjfx:javafx-controls:11:${platform}")
    }
}


publishToSonatype {
    groupId = Extras.group
    artifactId = Extras.id
    version = Extras.version

    name = Extras.name
    description = Extras.description
    url = Extras.url

    vendor = Extras.vendor
    vendorUrl = Extras.vendorUrl

    issueManagement {
        url = "${Extras.url}/issues"
        nickname = "Gitea Issues"
    }

    developer {
        id = "dorkbox"
        name = Extras.vendor
        email = "email@dorkbox.com"
    }
}
