# 应用图标设置指南

## 方式一：使用 Android Studio（推荐）

1. 打开 Android Studio
2. 右键点击 `app` → **New** → **Image Asset**
3. 选择 **Launcher Icons (Adaptive and Legacy)**
4. 上传你的图标图片（建议 512x512 PNG）
5. 点击 **Next** → **Finish**

自动生成的文件：
```
res/
├── mipmap-mdpi/ic_launcher.png
├── mipmap-hdpi/ic_launcher.png
├── mipmap-xhdpi/ic_launcher.png
├── mipmap-xxhdpi/ic_launcher.png
├── mipmap-xxxhdpi/ic_launcher.png
├── mipmap-mdpi/ic_launcher_round.png
├── mipmap-hdpi/ic_launcher_round.png
└── ...
```

## 方式二：手动添加

将不同尺寸的图标放入对应目录：

| 目录 | 尺寸 |
|------|------|
| `mipmap-mdpi/` | 48x48 |
| `mipmap-hdpi/` | 72x72 |
| `mipmap-xhdpi/` | 96x96 |
| `mipmap-xxhdpi/` | 144x144 |
| `mipmap-xxxhdpi/` | 192x192 |

每个目录放入 `ic_launcher.png` 和 `ic_launcher_round.png`

## 方式三：在线工具生成

使用 [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html)：
1. 上传图片
2. 调整样式
3. 下载 ZIP 包
4. 解压到 `app/src/main/res/`

## 临时方案（仅开发测试）

目前 AndroidManifest 引用的图标资源缺失，但应用仍可运行（会显示系统默认图标）。

**注意**：发布应用前必须添加正式图标。
