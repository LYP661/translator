# 安装指南 - 拍译 App

## 🎯 快速安装（推荐）

由于当前环境网络限制，推荐使用以下两种方式之一构建：

### 方式一：使用 Android Studio（最简单）

1. **下载项目**
   ```bash
   # 压缩项目目录
   cd /mnt/data/home/root/lyp_aut_260203
   tar -czf translator_app.tar.gz android_translator_app/

   # 或直接复制整个 android_translator_app 文件夹到 U 盘
   ```

2. **在有 Android Studio 的电脑上打开**
   - 解压 `translator_app.tar.gz`
   - Android Studio → File → Open → 选择 `android_translator_app` 目录
   - 等待 Gradle 同步完成（首次约 5 分钟）

3. **构建 APK**
   - 方式 A（UI）: `Build → Build Bundle(s) / APK(s) → Build APK(s)`
   - 方式 B（命令行）:
     ```bash
     cd android_translator_app
     ./gradlew assembleDebug
     ```

4. **APK 位置**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

### 方式二：在线构建（GitHub Actions）

如果有 GitHub 账号，可以使用 CI/CD 自动构建：

1. **创建 GitHub 仓库并推送代码**
   ```bash
   cd android_translator_app
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/YOUR_USERNAME/translator-app.git
   git push -u origin master
   ```

2. **创建 GitHub Actions 工作流**

   创建文件 `.github/workflows/build.yml`:
   ```yaml
   name: Build APK

   on:
     push:
       branches: [ master ]
     workflow_dispatch:

   jobs:
     build:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v3

         - name: Set up JDK 17
           uses: actions/setup-java@v3
           with:
             java-version: '17'
             distribution: 'temurin'

         - name: Grant execute permission for gradlew
           run: chmod +x gradlew

         - name: Build with Gradle
           run: ./gradlew assembleDebug

         - name: Upload APK
           uses: actions/upload-artifact@v3
           with:
             name: app-debug
             path: app/build/outputs/apk/debug/app-debug.apk
   ```

3. **下载构建好的 APK**
   - 进入仓库 → Actions → 选择最新的运行
   - 下载 Artifacts 中的 APK

---

### 方式三：命令行直接构建（需要 JDK 17 + Android SDK）

如果系统已安装 Java 和 Android SDK：

```bash
# 1. 检查环境
java -version  # 需要 JDK 17+
echo $ANDROID_HOME  # 需要设置 Android SDK 路径

# 2. 设置环境变量（如果没有）
export ANDROID_HOME=/path/to/android-sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# 3. 构建
cd android_translator_app
./gradlew assembleDebug

# 4. APK 输出
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 安装到手机

### 方式 A：USB 连接
```bash
# 1. 开启手机 USB 调试
# 设置 → 关于手机 → 连续点击版本号 7 次 → 返回 → 开发者选项 → USB 调试

# 2. 连接手机并安装
adb install app/build/outputs/apk/debug/app-debug.apk

# 或使用 Gradle
./gradlew installDebug
```

### 方式 B：手动安装
1. 将 APK 文件复制到手机（通过 USB、微信、QQ、网盘等）
2. 在手机文件管理器中找到 APK 文件
3. 点击 APK 文件
4. **允许安装未知来源应用**
   - MIUI: 设置 → 应用设置 → 安装未知应用 → 文件管理器 → 允许
   - EMUI: 设置 → 安全 → 更多安全设置 → 安装外部来源应用
   - ColorOS: 设置 → 其他设置 → 设备与隐私 → 安装外部来源应用
   - 原生 Android: 设置 → 应用和通知 → 特殊应用权限 → 安装未知应用
5. 点击**安装**

---

## 🛠️ 故障排除

### ❌ gradlew: Permission denied
```bash
chmod +x gradlew
```

### ❌ JAVA_HOME is not set
```bash
# Linux/macOS
export JAVA_HOME=/path/to/jdk-17
export PATH=$JAVA_HOME/bin:$PATH

# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
```

### ❌ SDK location not found
创建文件 `local.properties`:
```properties
sdk.dir=/path/to/android-sdk
```

### ❌ 手机安装失败"应用未安装"
- 检查是否已安装旧版本（先卸载）
- 确认系统版本 >= Android 8.0
- 检查存储空间是否充足

---

## 📦 文件清单

构建完成后，文件结构：
```
android_translator_app/
├── app/build/outputs/apk/debug/
│   └── app-debug.apk           ← 这是你要的安装包
├── README.md                    ← 项目说明
├── BUILD_GUIDE.md               ← 构建指南
├── INSTALL_GUIDE.md            ← 本文档
└── build-apk.sh                 ← Docker 构建脚本（可选）
```

---

## ✨ 首次使用

1. **安装后打开 App**
2. **授予相机权限**（拍照翻译功能需要）
3. **首次翻译时会下载模型**（约 30MB，需联网）
4. **之后可完全离线使用**

---

## 🔗 相关资源

- [Android Developer 文档](https://developer.android.com)
- [Gradle 构建指南](https://docs.gradle.org)
- [Android Studio 下载](https://developer.android.com/studio)
