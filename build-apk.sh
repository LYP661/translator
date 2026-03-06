#!/bin/bash
set -e

echo "================================================"
echo "    拍译 App - Docker 构建脚本"
echo "================================================"
echo ""

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_DIR="$PROJECT_DIR/output"

echo "📦 项目目录: $PROJECT_DIR"
echo "📤 输出目录: $OUTPUT_DIR"
echo ""

# 创建输出目录
mkdir -p "$OUTPUT_DIR"

echo "🐳 使用 Docker 构建 APK..."
echo "   镜像: mingc/android-build-box:latest"
echo ""

# 使用 Android build box 镜像构建
docker run --rm \
  -v "$PROJECT_DIR":/project \
  -w /project \
  mingc/android-build-box:latest \
  bash -c "
    echo '🔧 配置 Gradle 权限...'
    chmod +x ./gradlew

    echo '📥 下载依赖...'
    ./gradlew --no-daemon dependencies || true

    echo '🔨 构建 Debug APK...'
    ./gradlew --no-daemon assembleDebug

    echo '✅ 构建完成'
  "

# 复制 APK 到输出目录
APK_SOURCE="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_SOURCE" ]; then
    cp "$APK_SOURCE" "$OUTPUT_DIR/translator-v1.0.apk"
    echo ""
    echo "✨ 构建成功！"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  APK 文件位置:"
    echo "  $OUTPUT_DIR/translator-v1.0.apk"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "📱 安装方法:"
    echo "   1. 将 APK 文件复制到手机"
    echo "   2. 在手机上点击 APK 文件"
    echo "   3. 允许安装未知来源应用"
    echo "   4. 点击安装"
    echo ""
    ls -lh "$OUTPUT_DIR/translator-v1.0.apk"
else
    echo ""
    echo "❌ 构建失败: 未找到 APK 文件"
    echo "   请检查构建日志查找错误"
    exit 1
fi
