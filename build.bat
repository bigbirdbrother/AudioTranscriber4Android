@echo off
REM 清理构建缓存,打 Debug 包（自动使用 debug.keystore 签名）
.\gradlew clean && .\gradlew assembleDebug
REM 输出路径 app/build/outputs/apk/debug/app-debug.apk

