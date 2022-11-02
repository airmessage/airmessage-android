plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	id("kotlin-parcelize")
}

val isFullVariant = gradle.startParameter.taskRequests.any { request ->
	request.args.any { it.contains("Full") }
}
if(isFullVariant) {
	apply(plugin = "com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
	
	extensions.configure<com.google.android.libraries.mapsplatform.secrets_gradle_plugin.SecretsPluginExtension>("secrets") {
		propertiesFileName = "secrets.properties"
		defaultPropertiesFileName = "secrets.default.properties"
	}
}

android {
	namespace = "me.tagavari.airmessage.compose"
	compileSdk = 33
	
	apply(plugin = "org.jetbrains.kotlin.android")
	
	defaultConfig {
		minSdk = 23
		targetSdk = 33
		
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		consumerProguardFiles("consumer-rules.pro")
	}
	
	buildTypes {
		release {
			isMinifyEnabled = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}
	
	flavorDimensions += "distribution"
	productFlavors {
		create("full") {
			dimension = "distribution"
		}
		
		create("free") {
			dimension = "distribution"
		}
	}
	buildFeatures {
		compose = true
	}
	compileOptions {
		isCoreLibraryDesugaringEnabled = true
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	composeOptions {
		kotlinCompilerExtensionVersion = "1.3.2"
	}
	kotlinOptions {
		jvmTarget = JavaVersion.VERSION_11.toString()
	}
	lint {
		abortOnError = true
	}
}

dependencies {
	val fullImplementation by configurations
	
	coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.2.2")
	
	//Android extensions
	implementation("androidx.core:core-ktx:1.9.0")
	implementation("androidx.activity:activity-ktx:1.6.1")
	implementation("androidx.activity:activity-compose:1.6.1")
	implementation("androidx.fragment:fragment-ktx:1.5.4")
	implementation("androidx.preference:preference-ktx:1.2.0")
	implementation("androidx.browser:browser:1.4.0")
	implementation("androidx.exifinterface:exifinterface:1.3.5")
	implementation("androidx.security:security-crypto:1.0.0")
	implementation("androidx.window:window:1.0.0")
	
	//Material components
	implementation("com.google.android.material:material:1.7.0")
	
	//Jetpack Compose
	val composeBom = platform("androidx.compose:compose-bom:2022.10.00")
	implementation(composeBom)
	androidTestImplementation(composeBom)
	
	implementation("androidx.compose.material3:material3")
	implementation("androidx.compose.material3:material3-window-size-class")
	implementation("androidx.compose.material:material-icons-core")
	implementation("androidx.compose.material:material-icons-extended")
	implementation("androidx.compose.animation:animation")
	debugImplementation("androidx.compose.ui:ui-tooling")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("androidx.compose.runtime:runtime-rxjava3")
	androidTestImplementation("androidx.compose.ui:ui-test-junit4")
	
	//Accompanist
	implementation("com.google.accompanist:accompanist-flowlayout:0.26.5-rc")
	
	//Material motion
	implementation("io.github.fornewid:material-motion-compose-core:0.10.2-beta")
	
	//Coil
	implementation("io.coil-kt:coil-bom:2.2.2")
	implementation("io.coil-kt:coil")
	implementation("io.coil-kt:coil-compose")
	implementation("io.coil-kt:coil-gif")
	implementation("io.coil-kt:coil-video")
	
	//Coroutines
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx3:1.6.4")
	fullImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")
	
	//Android architecture components
	val lifecycleVersion = "2.5.1"
	
	// ViewModel
	implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
	// LiveData
	implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
	// Lifecycles only (without ViewModel or LiveData)
	implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
	// Compose
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
	
	//WorkManager
	val workVersion = "2.7.1"
	implementation("androidx.work:work-runtime-ktx:$workVersion")
	implementation("androidx.work:work-rxjava3:$workVersion")
	
	//ReactiveX
	implementation("io.reactivex.rxjava3:rxandroid:3.0.0")
	implementation("io.reactivex.rxjava3:rxjava:3.1.5")
	implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
	
	//Layouts and views
	implementation("androidx.constraintlayout:constraintlayout:2.1.4")
	implementation("androidx.viewpager2:viewpager2:1.0.0")
	implementation("com.google.android.flexbox:flexbox:3.0.0")
	
	implementation("nl.dionsegijn:konfetti-compose:2.0.2")
	implementation("com.github.chrisbanes:PhotoView:2.3.0")
	
	//ML Kit
	fullImplementation("com.google.mlkit:smart-reply:17.0.2")
	
	//Firebase
	fullImplementation(platform("com.google.firebase:firebase-bom:31.0.1"))
	fullImplementation("com.google.firebase:firebase-messaging-ktx")
	fullImplementation("com.google.firebase:firebase-auth-ktx")
	fullImplementation("com.google.firebase:firebase-analytics-ktx")
	fullImplementation("com.google.firebase:firebase-crashlytics-ktx")
	
	//GMS
	fullImplementation("com.google.android.gms:play-services-maps:18.1.0")
	fullImplementation("com.google.android.gms:play-services-location:20.0.0")
	fullImplementation("com.google.android.gms:play-services-auth:20.3.0")
	fullImplementation("com.google.maps.android:maps-ktx:3.4.0")
	
	//CameraX
	val cameraXVersion = "1.1.0"
	implementation("androidx.camera:camera-camera2:$cameraXVersion")
	implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
	implementation("androidx.camera:camera-view:$cameraXVersion")
	
	//About libraries
	val aboutLibsVersion = rootProject.extra["aboutlibs_version"]
	implementation("com.mikepenz:aboutlibraries-core:$aboutLibsVersion")
	
	//Tools
	implementation("com.googlecode.ez-vcard:ez-vcard:0.11.3")
	implementation("org.commonmark:commonmark:0.20.0")
	
	//Backend
	implementation("org.bouncycastle:bcprov-jdk15to18:1.72")
	
	implementation("com.github.bumptech.glide:glide:4.14.2")
	
	implementation("com.google.android.exoplayer:exoplayer-core:2.18.1")
	implementation("com.google.android.exoplayer:exoplayer-ui:2.18.1")
	
	implementation("me.saket.unfurl:unfurl:1.7.0")
	
	implementation(project(":android-smsmms"))
	
	implementation("org.java-websocket:Java-WebSocket:1.5.3")
	implementation("com.otaliastudios:transcoder:0.10.4")
}
