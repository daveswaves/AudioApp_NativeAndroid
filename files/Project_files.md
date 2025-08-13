## Lightweight, 100% command-line Kotlin + Gradle setup on Linux. Fully native Kotlin Android .apk

## Software Set up

* openjdk-17-jdk (Java Dev Kit).
* gradle-8.9-bin (Gradle).
* commandlinetools-linux-11076708_latest (Android SDK Command-Line Tools).

```sh
# Java JDK Setup
sudo apt update
sudo apt install openjdk-17-jdk unzip wget

java -version
```

```sh
# Gradle Setup
wget https://services.gradle.org/distributions/gradle-8.9-bin.zip
sudo mkdir /opt/gradle
sudo unzip -d /opt/gradle gradle-8.9-bin.zip
echo 'export PATH=$PATH:/opt/gradle/gradle-8.9/bin' >> ~/.bashrc
source ~/.bashrc

gradle -v
```

```sh
# Android SDK Command-Line Tools Setup
mkdir -p ~/Android/cmdline-tools
cd ~/Android/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-*.zip
mkdir latest
mv cmdline-tools/* latest/

# Add path to .bashrc
export ANDROID_HOME=$HOME/Android
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

source ~/.bashrc

# Install Required SDK Packages
sdkmanager --sdk_root=$ANDROID_HOME --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

## Project structure
```
MyApp/
 ├── app/
 │    ├── src/main/
 │    │         ├── AndroidManifest.xml
 │    │         ├── java/com/example/myapp/MainActivity.kt
 │    │         └── res/layout/activity_main.xml
 │    └── build.gradle.kts
 ├── build.gradle.kts
 ├── gradle.properties
 └── settings.gradle.kts
```

```sh
# build.gradle.kts
plugins {
    id("com.android.application") version "8.4.2" apply false
    kotlin("android") version "1.9.24" apply false
}
```

```sh
# gradle.properties
android.useAndroidX=true
android.enableJetifier=true
```

```sh
# settings.gradle.kts
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MyApp"
include(":app")
```

```sh
# app/build.gradle.kts
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.myapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapp"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
}
```

```sh
# app/src/main/AndroidManifest.xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="MyApp"
        android:theme="@style/Theme.MaterialComponents.DayNight.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
```

```sh
# app/src/main/java/com/example/myapp/MainActivity.kt
package com.example.myapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tv = findViewById<TextView>(R.id.textView)
        tv.text = "Hello from Kotlin CLI!"
    }
}
```

```sh
# app/src/main/res/layout/activity_main.xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical">

    <TextView
        android:id="@+id/textView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello World!"
        android:textSize="24sp"/>
</LinearLayout>
```

### Build the APK
```sh
gradle assembleDebug
# You’ll find the APK at: app/build/outputs/apk/debug/app-debug.apk

# If you need to fix any errors, use the following:
gradle clean assembleDebug
```

### '_Allow USB debugging_' phone setup
```sh
adb kill-server
adb start-server

# Connect phone via USB
# "Allow USB debugging?" message should appear on phone.
# Tick Always allow from this computer
# Tap Allow

# *** If phone restart occurs, Wireless debugging (Developer options)
# *** will turn itself off. Turn back on. Note that it will keep assigning dynamic port number.
# *** Select 'Wireless debugging' to view IP address and port.
# *** Run 'adb tcpip 5555' to fix port to 5555.
```

### PC operations:
```sh
# Verify authorization:
adb devices

# Something like 'R8YW501SJTE  device' should display
```

### Install the APK
```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
# -r  overwrites existing app

# App can now be opened from phone’s app launcher.
```

### Enabling Developer Options and USB Debugging on Phone
```sh
Settings > About phone > Software information
# Tap 'Build number' 7 times

Settings > Developer options
# Turn on 'USB debugging'
```

### NOTES

After PC boot, running `$ adb devices` will show something like:
```sh
* daemon not running; starting now at tcp:5037
* daemon started successfully
List of devices attached
```
After `daemon started`, connect phone (USB) and run command again `$ adb devices`.  
You should now see something like the following (assuming phone has been connected before):
```sh
List of devices attached
R8YW501SJTE	device
```
