# 快速开始 - 3 种方式生成 APK

## 🚀 方式一：Android Studio（推荐，最简单）

### 步骤：

1. **打包项目**
   ```bash
   cd /mnt/data/home/root/lyp_aut_260203
   tar -czf translator_app.tar.gz android_translator_app/
   # 或直接复制 android_translator_app 文件夹到 U 盘
   ```

2. **转移到有 Android Studio 的电脑**
   - 解压文件
   - Android Studio → File → Open → 选择项目目录

3. **构建 APK**
   - 点击菜单栏：`Build → Build Bundle(s) / APK(s) → Build APK(s)`
   - 等待 2-5 分钟
   - 弹出通知后点击 "locate" 查看 APK

4. **APK 位置**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

**时间**: 5-10 分钟（包括首次下载依赖）

---

## 🌐 方式二：GitHub Actions（在线构建，无需本地环境）

### 步骤：

1. **创建 GitHub 仓库**
   - 访问 https://github.com/new
   - 创建新仓库（public 或 private）

2. **推送代码**
   ```bash
   cd /mnt/data/home/root/lyp_aut_260203/android_translator_app
   git init
   git add .
   git commit -m "Initial commit: Translator App"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/translator-app.git
   git push -u origin main
   ```

3. **触发构建**
   - 推送代码后自动构建
   - 或进入仓库 → Actions → "Build Android APK" → "Run workflow"

4. **下载 APK**
   - Actions 页面 → 选择最新的成功运行
   - 滚动到底部 → Artifacts → 下载 `translator-app-debug.zip`
   - 解压得到 `app-debug.apk`

**时间**: 3-5 分钟（全自动）
**优点**: 无需本地配置，可随时重新构建

---

## 💻 方式三：命令行构建（需要 JDK 17 + Android SDK）

### 前置条件：
```bash
# 检查 Java 版本
java -version  # 需要 JDK 17 或更高

# 检查 Android SDK
echo $ANDROID_HOME  # 应输出 SDK 路径（如 /opt/android-sdk）
```

### 如果没有安装：

**安装 JDK 17**:
```bash
# Ubuntu/Debian
sudo apt update && sudo apt install openjdk-17-jdk

# macOS
brew install openjdk@17

# Windows
# 下载安装: https://adoptium.net/
```

**安装 Android SDK** (简化版):
```bash
# 下载 command line tools
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip -d ~/android-sdk
cd ~/android-sdk/cmdline-tools
mkdir latest && mv bin lib * latest/

# 设置环境变量
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# 安装必要组件
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### 构建 APK:
```bash
cd /mnt/data/home/root/lyp_aut_260203/android_translator_app

# 赋予执行权限
chmod +x gradlew

# 构建
./gradlew assembleDebug

# APK 位置
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

**时间**: 5-10 分钟（首次下载依赖）

---

## 📱 安装到手机

### 方法 A：USB 安装（推荐）
```bash
# 1. 开启手机 USB 调试
#    设置 → 关于手机 → 连续点 7 次版本号
#    → 返回 → 开发者选项 → 开启 USB 调试

# 2. 连接手机
adb devices  # 确认设备已连接

# 3. 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 方法 B：手动安装
1. 将 `app-debug.apk` 复制到手机（微信/QQ/网盘/USB）
2. 在手机上打开文件管理器
3. 点击 APK 文件
4. **允许安装未知来源**（根据提示操作）
5. 点击**安装**

---

## 🎯 推荐方案选择

| 场景 | 推荐方式 | 原因 |
|------|---------|------|
| 有 Android Studio | 方式一 | 最简单，可视化 |
| 无本地开发环境 | 方式二 | 完全在线，无需配置 |
| 熟悉命令行 | 方式三 | 灵活，可自动化 |
| 快速测试 | 方式一或二 | 节省配置时间 |
| CI/CD 集成 | 方式二 | 自动化构建 |

---

## ⚡ 常见问题

**Q: 构建失败怎么办？**
A: 查看详细日志，通常是网络问题（下载依赖失败）或 SDK 版本不匹配

**Q: 手机无法安装？**
A:
1. 检查系统版本 >= Android 8.0
2. 卸载旧版本（如果有）
3. 检查存储空间 > 100MB
4. 允许安装未知来源应用

**Q: APK 太大？**
A: Debug 版本未混淆压缩，约 20-30MB。Release 版本会更小。

**Q: 想要 Release 版本？**
A: 将命令改为 `./gradlew assembleRelease`（需配置签名）

---

## 📋 快速命令备忘

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建并安装到设备
./gradlew installDebug

# 清理构建
./gradlew clean

# 查看所有任务
./gradlew tasks

# 查看依赖
./gradlew dependencies
```

---

**选择你喜欢的方式开始构建吧！** 🎉
