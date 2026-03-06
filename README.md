# 拍译 - 安卓翻译 App

支持**拍照识别文字翻译**和**手动文字输入翻译**，均翻译成中文。

## 📚 快速导航

- **[⚡ 快速开始](QUICK_START.md)** - 3 种方式生成 APK（推荐从这里开始）⭐
- **[📱 安装指南](INSTALL_GUIDE.md)** - 详细的安装和故障排除
- **[🔨 构建指南](BUILD_GUIDE.md)** - 详细的构建和运行步骤
- **[🎨 图标设置](ICON_SETUP.md)** - 如何添加应用图标

## 核心功能

| 功能 | 说明 |
|------|------|
| 拍照翻译 | 对准文字拍照 → OCR 自动识别 → 翻译成中文 |
| 文字翻译 | 手动输入任意语言文字 → 翻译成中文 |
| 离线支持 | 翻译模型下载后可离线使用（约 30MB） |
| 一键复制 | 翻译结果可一键复制到剪贴板 |

## 技术栈

- **语言**: Kotlin
- **相机**: CameraX（官方推荐，兼容 Android 5+）
- **OCR**: ML Kit Text Recognition（设备端，识别拉丁/中文/日/韩文）
- **翻译**: ML Kit Translation（设备端，首次联网下载模型，之后离线可用）
- **UI**: Material Design 3，BottomNavigationView

## 构建方式

```bash
# 用 Android Studio 打开根目录
# 或命令行
./gradlew assembleDebug
```

**要求**: Android 8.0+ (API 26+)，需要相机权限

## 项目结构

```
app/src/main/
├── java/com/momenta/translator/
│   ├── ui/
│   │   ├── MainActivity.kt          # 底部导航容器
│   │   ├── CameraFragment.kt        # 拍照 + OCR + 翻译
│   │   └── TextInputFragment.kt     # 文字输入 + 翻译
│   └── viewmodel/
│       └── TranslateViewModel.kt    # 翻译逻辑（协程 + ML Kit）
└── res/
    ├── layout/
    │   ├── activity_main.xml
    │   ├── fragment_camera.xml
    │   └── fragment_text_input.xml
    ├── menu/bottom_nav_menu.xml
    └── values/{strings,themes}.xml
```

## 扩展计划

- [ ] 支持从相册选图
- [ ] 历史记录
- [ ] 多目标语言（不只是中文）
- [ ] 语音输入
- [ ] 翻译结果分享
