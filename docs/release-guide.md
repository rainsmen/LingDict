# LingDict 发布配置指南

> 版本：v1.0  
> 日期：2026-06-16  

---

## 📋 发布准备清单

### 1. 生成签名密钥

```bash
# 生成keystore文件
keytool -genkey -v -keystore lingdict-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias lingdict

# 输入密钥信息
# Store password: [输入密码]
# Key password: [输入密码，可与store password相同]
# 名字与姓氏: LingDict
# 组织单位: Development
# 组织: LingDict
# 城市: [your city]
# 州或省: [your province]
# 国家代码: CN
```

**注意**: 
- 妥善保管keystore文件和密码
- 丢失无法找回，将无法更新应用
- 建议备份到安全位置

---

### 2. 配置签名（方式A：本地配置）

创建 `keystore.properties` 文件（项目根目录）：

```properties
storeFile=lingdict-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=lingdict
keyPassword=YOUR_KEY_PASSWORD
```

更新 `app/build.gradle.kts`：

```kotlin
// 在android块之前
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    // ...
    
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

**重要**: 将keystore文件和配置添加到.gitignore：

```gitignore
*.jks
keystore.properties
```

---

### 3. 配置签名（方式B：环境变量）

使用环境变量（适合CI/CD）：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
}
```

设置环境变量：
```bash
export KEYSTORE_FILE=/path/to/lingdict-release.jks
export KEYSTORE_PASSWORD=your_password
export KEY_ALIAS=lingdict
export KEY_PASSWORD=your_password
```

---

### 4. 更新版本信息

编辑 `app/build.gradle.kts`：

```kotlin
android {
    defaultConfig {
        applicationId = "com.lingdict.app"
        versionCode = 1
        versionName = "1.0.0"
        // ...
    }
}
```

**版本规则**:
- `versionCode`: 每次发布递增（1, 2, 3...）
- `versionName`: 语义化版本（1.0.0, 1.0.1, 1.1.0...）

---

### 5. 构建Release APK

```bash
# 清理构建
./gradlew clean

# 构建Release APK
./gradlew assembleRelease

# 或构建App Bundle（推荐）
./gradlew bundleRelease

# 输出位置
# APK: app/build/outputs/apk/release/app-release.apk
# AAB: app/build/outputs/bundle/release/app-release.aab
```

---

### 6. 验证签名

```bash
# 验证APK签名
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# 查看签名信息
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

---

### 7. 测试Release版本

```bash
# 安装测试
adb install app/build/outputs/apk/release/app-release.apk

# 或通过Android Studio
# Run > Edit Configurations > Build Variant > release
```

**测试要点**:
- ✅ 所有功能正常
- ✅ 无Crash
- ✅ 性能良好
- ✅ ProGuard未破坏功能

---

## 📦 App Bundle vs APK

### App Bundle（推荐）

**优点**:
- 自动优化各设备APK
- 用户下载体积更小
- Google Play推荐格式

**缺点**:
- 只能通过Play Store分发
- 无法直接安装

**使用场景**: Google Play发布

### APK

**优点**:
- 可直接安装
- 可独立分发

**缺点**:
- 包含所有资源
- 体积较大

**使用场景**: 内部测试、第三方分发

---

## 🔐 隐私政策

创建隐私政策（必需）：

### 内容要点

1. **数据收集**
   - 学习记录（本地存储）
   - 不收集个人信息

2. **第三方服务**
   - 有道词典API（单词查询）
   - Pexels API（图片加载）

3. **权限使用**
   - 无需特殊权限
   - 网络权限（查询单词）

### 发布位置

选项1：GitHub Pages
```
https://[username].github.io/LingDict/privacy-policy.html
```

选项2：项目README
```
https://github.com/[username]/LingDict#privacy-policy
```

---

## 🖼️ 应用商店素材

### 应用图标

**规格**:
- 高分辨率：512x512 PNG（32位）
- 特色图片：1024x500 JPG/PNG

**要求**:
- 透明背景
- 无边框
- 清晰可辨

### 截图

**规格**:
- 手机：最少2张，最多8张
- 尺寸：16:9 或 9:16
- 最小边：320px
- 最大边：3840px

**建议**:
- 展示核心功能
- 添加说明文字
- 统一风格

### 应用描述

**简短描述**（80字符）:
```
智能生词学习应用，基于SM-2算法，高效背单词
```

**完整描述**（4000字符）:
```markdown
## LingDict - 智能生词学习应用

### 核心功能
🔍 **智能搜索** - 快速查询单词释义和例句
📚 **科学复习** - SM-2算法智能安排复习计划
🎴 **卡片学习** - 3D翻转卡片，沉浸式学习体验
✋ **手势操作** - 滑动标记掌握程度，操作便捷
🎤 **语音播放** - TTS发音，标准美式英语
📊 **学习统计** - 可视化学习进度和成果
🎯 **测试练习** - 4种题型，全面巩固记忆

### 特色亮点
- ✅ 77万词库，涵盖常用词汇
- ✅ 图片助记，加深记忆
- ✅ 深色模式，护眼舒适
- ✅ 无广告，纯净体验
- ✅ 离线可用，随时学习

### 适用人群
- 大学英语四六级备考
- 托福雅思考试准备
- 日常英语学习
- 词汇量提升

立即下载，开启高效背单词之旅！
```

---

## 📊 Google Play Console配置

### 1. 创建应用

1. 登录 [Play Console](https://play.google.com/console)
2. 创建应用
3. 填写基本信息

### 2. 商店信息

- 应用名称：LingDict
- 简短描述：[如上]
- 完整描述：[如上]
- 应用图标
- 特色图片
- 截图

### 3. 分类

- 应用类别：教育
- 标签：学习、教育、英语、词汇

### 4. 内容分级

- 目标年龄组：所有人
- 包含广告：否
- 包含应用内购买：否

### 5. 隐私政策

- 隐私政策URL：[your privacy policy URL]

---

## 🚀 发布流程

### 内部测试

1. **创建测试Track**
   - Play Console > 测试 > 内部测试
   - 创建新版本
   
2. **上传APK/AAB**
   - 拖放app-release.aab
   - 填写版本说明
   
3. **邀请测试者**
   - 添加邮箱地址
   - 发送测试链接

### Beta测试

1. **晋升到Beta**
   - 内部测试通过后
   - 晋升到封闭式测试
   
2. **扩大测试范围**
   - 邀请更多测试者
   - 收集反馈

### 正式发布

1. **晋升到生产**
   - Beta测试通过
   - 晋升到生产Track
   
2. **分阶段发布**
   - 10% → 50% → 100%
   - 监控Crash率
   
3. **审核**
   - 等待Google审核（1-3天）
   - 修复审核反馈

---

## ⚠️ 注意事项

1. **密钥安全**
   - 妥善保管keystore
   - 不要提交到Git
   - 定期备份

2. **版本管理**
   - versionCode必须递增
   - 遵循语义化版本

3. **测试充分**
   - Release版本必须测试
   - 验证ProGuard效果
   - 检查Crash

4. **合规性**
   - 遵守Google政策
   - 提供隐私政策
   - 声明权限用途

---

## 📝 版本发布记录

### v1.0.0 (Build 1) - 2026-06-16

**首次发布**

功能：
- ✅ 单词搜索和查询
- ✅ 智能复习系统
- ✅ 卡片学习模式
- ✅ 测试练习功能
- ✅ 学习统计图表
- ✅ TTS语音播放
- ✅ 图片助记

---

**文档版本**: v1.0  
**更新日期**: 2026-06-16  
**作者**: LingDict Dev Team
