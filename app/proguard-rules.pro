# Ichiji has no third-party libraries and uses no reflection, so R8's defaults
# (plus the keep rules generated from AndroidManifest.xml) are enough.
# Keep class and method names so crash reports in Play Console are readable
# without uploading a mapping file.
-dontobfuscate
