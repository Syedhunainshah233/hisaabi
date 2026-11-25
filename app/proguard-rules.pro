# Hisaabi R8 rules.
# Room entities and DAO are used by generated code; keep them readable for safety.
-keep class com.syedhunain.hisaabi.data.** { *; }
# Keep line numbers so crash reports in Play Console can be read (the mapping file is in the bundle).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
