plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
}

extra.set("PUBLISH_GROUP_ID", "com.github.vulturelan")
extra.set("PUBLISH_VERSION", "1.0.0")
extra.set("PUBLISH_ARTIFACT_ID", "compressor")

val PUBLISH_VERSION: String by extra

afterEvaluate {
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.vulturelan"
                artifactId = "compressor"
                version = "1.0.2"
            }
        }
    }
}

android {
    namespace = "id.zelory.compressor"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
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

    publishing {
        singleVariant("release")
    }
}

dependencies {
    val kotlin_version: String by rootProject.extra
    val kotlin_coroutines_version: String by rootProject.extra

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlin_coroutines_version")
    implementation("androidx.exifinterface:exifinterface:1.4.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("com.natpryce:hamkrest:1.8.0.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlin_coroutines_version")
}
