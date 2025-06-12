# 清理构建缓存
./gradlew clean

# 打 Debug 包（自动使用 debug.keystore 签名）
./gradlew assembleDebug

# 打 Release 包（需提前配置签名）
#./gradlew assembleRelease

#输出路径 app/build/outputs/apk/debug/app-debug.apk