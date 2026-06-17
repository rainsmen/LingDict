#!/bin/bash

# LingDict 构建脚本
# 使用说明: ./build_guide.sh

set -e

echo "========================================"
echo "  LingDict 构建指南"
echo "========================================"
echo ""

# 检查是否有Android SDK
if [ -z "$ANDROID_HOME" ] && [ ! -d "$HOME/Android/Sdk" ]; then
    echo "❌ 未检测到Android SDK"
    echo ""
    echo "请先安装Android SDK，有以下几种方式："
    echo ""
    echo "1. 安装Android Studio（推荐）:"
    echo "   - 下载: https://developer.android.com/studio"
    echo "   - 安装后SDK会自动配置"
    echo ""
    echo "2. 只安装命令行工具:"
    echo "   - 下载: https://developer.android.com/studio#command-tools"
    echo "   - 解压到 ~/Android/Sdk/cmdline-tools/latest/"
    echo "   - 运行: sdkmanager \"platform-tools\" \"platforms;android-34\" \"build-tools;34.0.0\""
    echo ""
    echo "3. 如果已安装SDK但路径不同，请更新local.properties中的sdk.dir"
    echo ""
    exit 1
fi

# 设置SDK路径
if [ -n "$ANDROID_HOME" ]; then
    SDK_PATH="$ANDROID_HOME"
elif [ -d "$HOME/Android/Sdk" ]; then
    SDK_PATH="$HOME/Android/Sdk"
    export ANDROID_HOME="$SDK_PATH"
fi

echo "✅ 检测到Android SDK: $SDK_PATH"
echo ""

# 检查必要的SDK组件
echo "检查SDK组件..."

REQUIRED_COMPONENTS=(
    "platforms;android-34"
    "build-tools;34.0.0"
    "platform-tools"
)

MISSING_COMPONENTS=()

for component in "${REQUIRED_COMPONENTS[@]}"; do
    COMPONENT_PATH="${component//;/\/}"
    if [ ! -d "$SDK_PATH/$COMPONENT_PATH" ]; then
        MISSING_COMPONENTS+=("$component")
    fi
done

if [ ${#MISSING_COMPONENTS[@]} -gt 0 ]; then
    echo "❌ 缺少以下SDK组件:"
    for component in "${MISSING_COMPONENTS[@]}"; do
        echo "   - $component"
    done
    echo ""
    echo "请使用Android Studio的SDK Manager安装，或运行："
    echo "sdkmanager ${MISSING_COMPONENTS[@]}"
    echo ""
    exit 1
fi

echo "✅ 所有必需的SDK组件已安装"
echo ""

# 检查local.properties
echo "检查配置文件..."
if [ ! -f "local.properties" ]; then
    echo "❌ local.properties 文件不存在"
    echo "创建配置文件..."
    cat > local.properties << EOF
# API Keys
YOUDAO_APP_KEY=5f2d7eb0cf9f328f
YOUDAO_APP_SECRET=tTtqujYlNqaBFJlkVQqheKLcX976InDe
PEXELS_API_KEY=r8EusdndsAEQsXT074L3nxbPvR0Z6qidQIGXfReWPARtpvXqSrC2lKbr

# Android SDK路径
sdk.dir=$SDK_PATH
EOF
    echo "✅ 已创建 local.properties"
else
    # 更新sdk.dir
    if grep -q "^sdk.dir=" local.properties; then
        sed -i "s|^sdk.dir=.*|sdk.dir=$SDK_PATH|" local.properties
        echo "✅ 已更新 local.properties 中的SDK路径"
    else
        echo "sdk.dir=$SDK_PATH" >> local.properties
        echo "✅ 已添加SDK路径到 local.properties"
    fi
fi
echo ""

# 开始构建
echo "========================================"
echo "  开始构建应用"
echo "========================================"
echo ""

echo "清理旧的构建..."
./gradlew clean

echo ""
echo "构建Debug版本..."
./gradlew assembleDebug

echo ""
echo "========================================"
echo "  ✅ 构建成功！"
echo "========================================"
echo ""
echo "APK位置: app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "安装到设备:"
echo "  adb install app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "或运行:"
echo "  ./gradlew installDebug"
echo ""
