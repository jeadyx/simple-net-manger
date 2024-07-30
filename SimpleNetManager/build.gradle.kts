plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
//    id("org.jetbrains.dokka") version "1.9.20"
    alias(libs.plugins.sonatype.publish)
}

android {
    namespace = "io.github.jeadyx.simplenetmanager"
    compileSdk = 34

    defaultConfig {
        minSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
group = "io.github.jeadyx.compose"
version = "1.3"
val tokenUsername:String by project
val tokenPassword:String by project
sonatypeUploader{
    bundleName = "SimpleNetManger-$version"
    tokenName = tokenUsername
    tokenPasswd = tokenPassword
    pom = Action<MavenPom>{
        name.set("SimpleNetManger")
        description.set("A simple network manager api")
        url.set("https://github.com/jeadyx/GitVersionManager")
        scm {
            connection.set("scm:git:git://github.com/jeadyx/SimpleNetManger.git")
            developerConnection.set("scm:git:ssh://github.com/jeadyx/SimpleNetManger.git")
            url.set("https://github.com/jeadyx/SimpleNetManger")
        }
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id.set("jeadyx")
                name.set("Jeady")
            }
        }
        withXml {
            val dependenciesNode = asNode().appendNode("dependencies")
            val dependencyOkhttp = dependenciesNode.appendNode("dependency")
            dependencyOkhttp.appendNode("groupId", "com.squareup.okhttp3")
            dependencyOkhttp.appendNode("artifactId", "okhttp")
            dependencyOkhttp.appendNode("version", "4.12.0")
            val dependencyGson = dependenciesNode.appendNode("dependency")
            dependencyGson.appendNode("groupId", "com.google.code.gson")
            dependencyGson.appendNode("artifactId", "gson")
            dependencyGson.appendNode("version", "2.10.1")
        }
    }
}
dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}