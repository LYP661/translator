# 构建和运行指南

## 📋 前置要求

- **Android Studio** (推荐最新稳定版，Arctic Fox+)
- **JDK 17** 或更高版本
- **Android SDK** API 34 (Android 14)
- **最低支持设备**: Android 8.0 (API 26)

## 🚀 快速开始

### 方式一：使用 Android Studio（推荐）

1. **打开项目**
   ```bash
   # 用 Android Studio 打开当前目录
   File → Open → 选择 android_translator_app 文件夹
   ```

2. **等待 Gradle 同步**
   - 首次打开会自动下载依赖（约 2-5 分钟）
   - 如果失败，点击 `File → Sync Project with Gradle Files`

3. **配置应用图标**（可选）
   - 参考 [ICON_SETUP.md](ICON_SETUP.md)
   - 或暂时跳过（会显示系统默认图标）

4. **连接设备或启动模拟器**
   - **真机**: USB 连接 + 开启开发者选项 + USB 调试
   - **模拟器**: `Tools → Device Manager → Create Device`

5. **运行**
   - 点击绿色 ▶️ 按钮（Run 'app'）
   - 或按 `Shift + F10`

### 方式二：命令行构建

```bash
# 1. 进入项目目录
cd android_translator_app

# 2. 赋予执行权限（首次）
chmod +x gradlew

# 3. 构建 Debug APK
./gradlew assembleDebug

# 4. APK 输出路径
# app/build/outputs/apk/debug/app-debug.apk

# 5. 安装到已连接设备
./gradlew installDebug

# 6. 构建 Release APK（需配置签名）
./gradlew assembleRelease
```

## 📦 生成 APK

### Debug 版本（开发测试）
```bash
./gradlew assembleDebug
# 输出: app/build/outputs/apk/debug/app-debug.apk
```

### Release 版本（正式发布）

1. **创建签名密钥**
   ```bash
   keytool -genkey -v -keystore release.keystore \
     -alias translator -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **配置签名**（在 `app/build.gradle.kts` 添加）
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file("../release.keystore")
               storePassword = "your_password"
               keyAlias = "translator"
               keyPassword = "your_password"
           }
       }
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
               // ...
           }
       }
   }
   ```

3. **构建**
   ```bash
   ./gradlew assembleRelease
   # 输出: app/build/outputs/apk/release/app-release.apk
   ```

## 🧪 运行测试

```bash
# 单元测试
./gradlew test

# 仪器测试（需连接设备）
./gradlew connectedAndroidTest

# 生成测试报告
./gradlew testDebugUnitTest --tests '*'
```

## 🐛 常见问题

### ❌ SDK 版本错误
```
Could not find com.android.tools.build:gradle:8.2.0
```
**解决**: 打开 Android Studio → `Tools → SDK Manager` → 确保已安装 SDK 34

### ❌ JDK 版本不匹配
```
Unsupported class file major version 61
```
**解决**: 设置 JDK 17+
```bash
# macOS/Linux
export JAVA_HOME=/path/to/jdk-17

# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-17
```

### ❌ 相机权限被拒绝
**现象**: 拍照功能无法使用

**解决**:
1. 设置 → 应用 → 拍译 → 权限 → 相机 → 允许
2. 或卸载重装，首次使用时授权

### ❌ 翻译失败
```
翻译失败，请检查网络（首次需下载模型）
```
**解决**: 首次使用需联网下载 ML Kit 模型（约 30MB），之后可离线使用

## 📱 支持的设备

| 要求 | 说明 |
|------|------|
| **最低系统** | Android 8.0 (API 26) |
| **推荐系统** | Android 10+ |
| **相机** | 任何后置摄像头（可选前置） |
| **存储** | 约 100MB（含模型） |
| **网络** | 仅首次下载模型需要 |

## 📚 依赖库

- **CameraX**: 1.3.1
- **ML Kit Text Recognition**: 16.0.0
- **ML Kit Translate**: 17.0.2
- **Material Components**: 1.11.0
- **AndroidX Lifecycle**: 2.7.0

## 🔗 相关资源

- [Android Developer 官方文档](https://developer.android.com)
- [CameraX 指南](https://developer.android.com/training/camerax)
- [ML Kit 文档](https://developers.google.com/ml-kit)
- [Material Design 3](https://m3.material.io)
