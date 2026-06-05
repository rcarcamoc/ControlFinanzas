# Rules to ignore warnings about missing Java SE desktop/server classes on Android
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.batik.**
-dontwarn org.osgi.framework.**
-dontwarn org.apache.logging.log4j.**

# Ignore warnings for Apache POI, XMLBeans, and Commons Collections
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.apache.commons.collections4.**

# Keep Apache POI and XMLBeans classes to prevent issues with reflection
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.apache.commons.collections4.** { *; }