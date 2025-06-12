# Audio Transcriber 音频转文字应用
> 本 Readme 由 AI 生成，内容仅供参考，请仔细甄别。
<img src="https://github.com/user-attachments/assets/472dddcf-20f7-42d7-af29-b4b3e5e0c5b0" alt="介绍" width="400"/>

## 功能特性

- 专为 NAS 而生，不能独立运行
- 非实时性的录音转文字
- 本地音频文件转文字
- 历史记录管理
- 必需配置后端服务地址
- Material Design 3 界面设计

## 技术栈

- Kotlin
- Jetpack Compose
- Room 数据库
- OkHttp 网络请求
- AndroidX 核心组件

## 构建要求

- Android Studio Giraffe | 2022.3.1+
- JDK 11+
- Android Gradle Plugin 8.0+
- Gradle 8.0+

## 安装指南

1. 克隆仓库：  
   ```bash
   git clone https://github.com/bigbirdbrother/audio-transcriber.git
   ```
2. 打开项目：  
   使用 Android Studio 打开项目根目录

3. 构建应用：  
   ```bash
   ./build.sh
   ```

## 项目结构

项目路径：`app/src/main/java/com/bigbirdbrother/recordaudio/`

包含目录：
- `res/` - 资源文件

## 依赖库

- AndroidX Core KTX
- Jetpack Compose
- Room Persistence
- OkHttp
- Material Design 3

## 贡献指南

在提交 Pull Request 之前请确保：

1. 代码符合 Kotlin 风格指南
2. 新功能包含单元测试
3. 更新相关文档

## 许可证

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

## 联系方式

暂无
