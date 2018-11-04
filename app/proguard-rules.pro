# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#
# android stuff
#

-keep public class * extends android.preference.PreferenceFragment
-keep public class * extends android.content.BroadcastReceiver
-keep class android.support.v7.widget.SearchView { *; }

#
# generic stuff
#
-dontoptimize
-printmapping out.map
-keepattributes SourceFile,LineNumberTable
-keepnames class mobi.upod.**

#
# scala
#
-dontwarn scala.**

-keepclassmembers class * {
    ** MODULE$;
}

-keepclassmembers class * extends scala.Enumeration {
   *;
}

-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinPool {
    long eventCount;
    int  workerCounts;
    int  runControl;
    scala.concurrent.forkjoin.ForkJoinPool$WaitQueueNode syncStack;
    scala.concurrent.forkjoin.ForkJoinPool$WaitQueueNode spareStack;
}

-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinWorkerThread {
    int base;
    int sp;
    int runState;
}

-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinTask {
    int status;
}

-keepclassmembernames class scala.concurrent.forkjoin.LinkedTransferQueue {
    scala.concurrent.forkjoin.LinkedTransferQueue$PaddedAtomicReference head;
    scala.concurrent.forkjoin.LinkedTransferQueue$PaddedAtomicReference tail;
    scala.concurrent.forkjoin.LinkedTransferQueue$PaddedAtomicReference cleanMe;
}

-keep class scala.collection.SeqLike {
    public protected *;
}

# Needed by google-api-client to keep generic types and @Key annotations accessed via reflection

-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}

-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

# Needed by Guava

-dontwarn sun.misc.Unsafe

# See https://groups.google.com/forum/#!topic/guava-discuss/YCZzeCiIVoI
-dontwarn com.google.common.collect.MinMaxPriorityQueue

# Needed by logback
-keep class ch.qos.** { *; }
-keep class org.slf4j.** { *; }
-keepattributes *Annotation*
-dontwarn ch.qos.logback.core.net.*

# Rome RSS
-keep class com.google.code.rome.android.repackaged.com.sun.syndication.** { *; }
-keep class org.jdom.** { *; }
-dontwarn com.google.code.rome.android.repackaged.**
-dontwarn org.jdom.**

# mp4parser
-keep class com.coremedia.iso.**
-keep class com.googlecode.mp4parser.**
-dontwarn javax.imageio.**
-dontwarn java.awt.image.**

# evernote android-job
-dontwarn com.evernote.android.job.gcm.**
-dontwarn com.evernote.android.job.util.GcmAvailableHelper

-keep public class com.evernote.android.job.v21.PlatformJobService
-keep public class com.evernote.android.job.v14.PlatformAlarmService
-keep public class com.evernote.android.job.v14.PlatformAlarmReceiver
-keep public class com.evernote.android.job.JobBootReceiver

# other stuff
-keep class org.joda.time.DateTimeZone { *; }
-keep class org.joda.time.DateTimeZone$* { *; }
-keep class org.joda.time.tz.** { *; }
-keep class mobi.upod.app.gui.cast.MediaRouteActionProvider { *; }

# keep my binding module which is referenced only via reflection
-keep class mobi.upod.app.AppBindingModule { *; }
