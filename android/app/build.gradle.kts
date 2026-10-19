plugins { id("com.android.application") }

android { namespace = "com.sbp95.firetvstreaming"; compileSdk = 35
  defaultConfig { applicationId = "com.sbp95.firetvstreaming"; minSdk = 23; targetSdk = 35; versionCode = 4; versionName = "0.1.4" }
}
dependencies { implementation("androidx.core:core-ktx:1.15.0"); implementation("androidx.media3:media3-exoplayer:1.5.1"); implementation("androidx.media3:media3-ui:1.5.1") }
