plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "it.socialblock"
    compileSdk = 36

    defaultConfig {
        applicationId = "it.socialblock"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    // Release signing is configured only when the keystore is provided through environment
    // variables (as the GitHub release workflow does); otherwise release builds stay unsigned.
    val releaseKeystorePath = System.getenv("SOCIALBLOCK_KEYSTORE_PATH")
    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("SOCIALBLOCK_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SOCIALBLOCK_KEY_ALIAS")
                keyPassword = System.getenv("SOCIALBLOCK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/LICENSE.md",
            "/META-INF/LICENSE-notice.md",
        )
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = true
        // API 37 is not yet published by the configured Android SDK repository.
        disable += setOf(
            "AndroidGradlePluginVersion",
            "GradleDependency",
            "NewerVersionAvailable",
            "OldTargetApi",
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

ktlint {
    android.set(true)
    version.set("1.8.0")
    filter {
        exclude("**/generated/**")
    }
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

val kotlinSources = fileTree("src") {
    include("**/*.kt")
    exclude("**/generated/**")
}

val ktlintCli by configurations.creating

val ktlintSourceCheck by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks Kotlin source files with ktlint."
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    args(kotlinSources.files.sorted().map { it.absolutePath })
}

val ktlintSourceFormat by tasks.registering(JavaExec::class) {
    group = "formatting"
    description = "Formats Kotlin source files with ktlint."
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    args("-F")
    args(kotlinSources.files.sorted().map { it.absolutePath })
}

ktlintSourceCheck.configure {
    mustRunAfter(ktlintSourceFormat)
}

tasks.named("ktlintCheck") {
    dependsOn(ktlintSourceCheck)
}

tasks.named("ktlintFormat") {
    dependsOn(ktlintSourceFormat)
}

dependencies {
    ktlintCli(libs.ktlint.cli)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
