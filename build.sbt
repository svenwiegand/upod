import android.Keys._
import android.DebugSigningConfig
import android.PlainSigningConfig

val androidBuildToolsVersion = Some("26.0.1")
val androidPlatformTarget = "android-25"
val androidMinSdkVersion = "21" // should be 14 (21 for debug because of multi dex support)

//
// android libraries
//

val bottomSheet = project.in(file("bottom-sheet")).
  settings(androidBuildAar: _*).
  settings(
    name := "bottom-sheet",

    buildToolsVersion in Android := androidBuildToolsVersion,
    platformTarget in Android := androidPlatformTarget,
    minSdkVersion in Android := androidMinSdkVersion,
    javacOptions in Compile ++= List(
      "-source",
      "1.7",
      "-target",
      "1.7"),

    libraryDependencies ++= List(
      aar("com.android.support" % "appcompat-v7" % "25.3.1"),
      aar("com.android.support" % "design" % "25.3.1"),
      aar("com.android.support" % "support-v4" % "25.3.1")
    )
  )

val dragSortListView = project.in(file("drag-sort-listview")).
  settings(androidBuildAar: _*).
  settings(
    buildToolsVersion in Android := androidBuildToolsVersion,
    platformTarget in Android := androidPlatformTarget,
    minSdkVersion in Android := androidMinSdkVersion,
    javacOptions in Compile ++= List(
      "-source",
      "1.7",
      "-target",
      "1.7"),

    antLayoutDetector in Android := (),

    libraryDependencies ++= List(
      aar("com.android.support" % "appcompat-v7" % "25.3.1"),
      aar("com.android.support" % "support-v4" % "25.3.1")
    )
  )

val prestissimo = project.in(file("prestissimo")).
  settings(androidBuildAar: _*).
  settings(
    buildToolsVersion in Android := androidBuildToolsVersion,
    platformTarget in Android := androidPlatformTarget,
    minSdkVersion in Android := androidMinSdkVersion,
    javacOptions in Compile ++= List(
      "-source",
      "1.7",
      "-target",
      "1.7"),

    antLayoutDetector in Android := (),

    libraryDependencies ++= List(
      "com.github.tony19" % "logback-android-core" % "1.1.1-4" exclude("com.google.android", "android"),
      "com.github.tony19" % "logback-android-classic" % "1.1.1-4" exclude("com.google.android", "android"),
      "org.slf4j" % "slf4j-api" % "1.7.6"
    )
  )

val showcaseView = project.in(file("showcase-view")).
  settings(androidBuildAar: _*).
  settings(
    buildToolsVersion in Android := androidBuildToolsVersion,
    platformTarget in Android := androidPlatformTarget,
    minSdkVersion in Android := androidMinSdkVersion,
    javacOptions in Compile ++= List(
      "-source",
      "1.7",
      "-target",
      "1.7"),

    libraryDependencies ++= List()
  )

//
// libraries
//

val fixMissingMediarouterInnerClasses = project.in(file("fix-missing-mediarouter-inner-classes"))

//
// apps
//

val app = project.in(file("app")).
  androidBuildWith(dragSortListView, prestissimo, showcaseView, bottomSheet).
  //enablePlugins(AndroidProtify).
  settings(
    versionCode in Android := Some(6100),
    versionName in Android := Some("6.1.0"),
    buildToolsVersion in Android := androidBuildToolsVersion,
    platformTarget in Android := androidPlatformTarget,
    minSdkVersion in Android := androidMinSdkVersion,

    antLayoutDetector in Android := (),

    unmanagedResourceDirectories in Compile := Seq(baseDirectory.value / "src"),
    includeFilter in unmanagedResources := "*.properties",

    javacOptions in Compile ++= List(
      "-source",
      "1.7",
      "-target",
      "1.7"),
    scalaVersion := "2.11.6",

    resConfigs in Android := Seq("en", "de"),

    apkDebugSigningConfig in Android := DebugSigningConfig(
      keystore = new File("keystore-debug.jks")
    ),
    apkSigningConfig in Android := Option(
      PlainSigningConfig(
        keystore = new File("keystore.jks"),
        storePass = "???",
        alias = "???",
        keyPass = Some("???")
      )
    ),

    libraryDependencies ++= List(
      aar("mobi.upod" % "time-duration-picker" % "1.0.3"),

      "com.escalatesoft.subcut" % "subcut_2.11" % "2.1",
      "com.evernote" % "android-job" % "1.1.2",
      "com.google.http-client" % "google-http-client-android" % "1.20.0",
      "com.google.api-client" % "google-api-client-android" % "1.22.0",
      "com.google.apis" % "google-api-services-drive" % "v3-rev78-1.22.0",
      "com.google.code.gson" % "gson" % "2.3.1",
      "com.googlecode.mp4parser" % "isoparser" % "1.1.17",
      "com.github.nscala-time" % "nscala-time_2.11" % "2.0.0",
      "com.nostra13.universalimageloader" % "universal-image-loader" % "1.8.5",

      "com.github.tony19" % "logback-android-core" % "1.1.1-4" exclude("com.google.android", "android"),
      "com.github.tony19" % "logback-android-classic" % "1.1.1-4" exclude("com.google.android", "android"),
      "org.slf4j" % "slf4j-api" % "1.7.6",

      aar("com.crashlytics.sdk.android" % "crashlytics" % "2.5.2"),

      aar("com.google.android.gms" % "play-services-cast" % "10.2.0"),
      aar("com.google.android.gms" % "play-services-auth" % "10.2.0"),
      aar("com.google.android.gms" % "play-services-drive" % "10.2.0"),
      aar("com.google.firebase" % "firebase-messaging" % "10.2.0"),
      aar("com.android.support" % "support-v13" % "25.3.1"),
      aar("com.android.support" % "design" % "25.3.1"),
      aar("com.android.support" % "cardview-v7" % "25.3.1"),
      aar("com.android.support" % "mediarouter-v7" % "25.3.1"),
      aar("com.android.support" % "appcompat-v7" % "25.3.1"),
      "com.android.support" % "multidex" % "1.0.1"
    ),

    googleServicesSettings,

    useProguardInDebug in Android := false,

    dexMulti in Android := true,
    dexMainClassesConfig in Android := baseDirectory.value / "maindexlist.txt",

    packagingOptions in Android := PackagingOptions(
      excludes = Seq(
        "META-INF/DEPENDENCIES",
        "META-INF/LICENSE",
        "META-INF/LICENSE.txt",
        "META-INF/license.txt",
        "META-INF/NOTICE",
        "META-INF/NOTICE.txt",
        "META-INF/notice.txt",
        "META-INF/ASL2.0",
        "rootdoc.txt"
      )
    )
  )

val license = project.in(file("license")).
  settings(androidBuild).
  settings(
    versionCode in Android := Some(5),
    versionName in Android := Some("4.0.1"),
    buildToolsVersion in Android := androidBuildToolsVersion,
    platformTarget in Android := androidPlatformTarget,
    minSdkVersion in Android := androidMinSdkVersion,

    apkDebugSigningConfig in Android := DebugSigningConfig(
      keystore = new File("keystore-debug.jks")
    ),
    apkSigningConfig in Android := Option(
      PlainSigningConfig(
        keystore = new File("keystore.jks"),
        storePass = "???",
        alias = "???",
        keyPass = Some("???")
      )
    ),

    antLayoutDetector in Android := ()
  )