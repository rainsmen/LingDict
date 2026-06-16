# LingDict - 生词查询背诵应用

![Android CI](https://github.com/rainsmen/LingDict/workflows/Android%20CI/badge.svg)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

LingDict 是一款与传统单词背诵软件不同的学习应用。它专注于**查询式学习**——用户主动查询不认识的单词并加入生词库学习，而非被动接受词库推荐。

## ✨ 核心特性

- 🔍 **智能单词查询** - 自动补全提示，快速找到目标单词
- 📚 **混合词典数据源** - 优先使用ECDICT离线词库（77万词条），未找到时自动调用有道API
- 🎴 **3D翻转卡片** - 精美的卡片式单词展示，支持3D翻转动画
- 👆 **滑动手势学习** - 左滑"不认识"、右滑"认识"、上滑"收藏"
- 🧠 **SM-2算法** - 科学的间隔重复算法，智能安排复习计划
- 📝 **多样化测试** - 选择题、填空题、听力题、判断题四种题型
- 📊 **可视化统计** - 折线图、热力图、饼图展示学习进度
- 🎨 **助记图片** - 集成Pexels API，为单词匹配助记图片
- 🔊 **TTS发音** - 支持单词发音播放
- 📄 **PDF导出** - 将生词库导出为PDF格式

## 📱 系统要求

- Android 10 (API 29) 及以上
- 64位设备（arm64-v8a 或 x86_64）

## 🏗️ 技术栈

- **语言**: Kotlin 1.9+
- **UI**: Jetpack Compose + Material 3
- **架构**: MVVM + Clean Architecture
- **依赖注入**: Hilt
- **数据库**: Room
- **网络**: Retrofit + OkHttp
- **图片加载**: Coil
- **异步处理**: Coroutines + Flow

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/rainsmen/LingDict.git
cd LingDict
```

### 2. 配置API密钥

复制 `local.properties.example` 为 `local.properties`，并填写你的API密钥：

```properties
YOUDAO_APP_KEY=your_youdao_app_key_here
YOUDAO_APP_SECRET=your_youdao_app_secret_here
PEXELS_API_KEY=your_pexels_api_key_here
```

**获取API密钥：**
- 有道智云API：https://ai.youdao.com/
- Pexels API：https://www.pexels.com/api/

### 3. 构建运行

```bash
# 使用Android Studio打开项目，或者命令行构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

## 📦 项目结构

```
app/
├── data/              # 数据层
│   ├── local/        # Room数据库
│   ├── remote/       # API服务
│   └── repository/   # 仓库实现
├── domain/           # 领域层
│   ├── model/       # 领域模型
│   ├── repository/  # 仓库接口
│   └── usecase/     # 用例
├── presentation/     # 表示层
│   ├── home/        # 首页
│   ├── word/        # 单词详情
│   ├── learn/       # 学习页面
│   ├── test/        # 测试页面
│   ├── statistics/  # 统计页面
│   └── settings/    # 设置页面
├── di/              # 依赖注入模块
└── util/            # 工具类
```

## 🔧 GitHub Actions

本项目配置了CI/CD自动构建：

- **自动构建**: 每次push到main/develop分支时自动编译
- **单元测试**: 自动运行测试用例
- **APK产物**: 构建产物会自动上传到GitHub Artifacts

**配置Secrets**：
在GitHub仓库的 Settings → Secrets and variables → Actions 中添加：
- `YOUDAO_APP_KEY`
- `YOUDAO_APP_SECRET`
- `PEXELS_API_KEY`

## 📖 开发文档

详细的开发文档请参考：
- [可行性评估报告](docs/feasibility-report.md)
- [开发指南](docs/development-guide.md)

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 开源协议

本项目采用 MIT 协议开源 - 详见 [LICENSE](LICENSE) 文件

## 👨‍💻 作者

**LingDict Development Team**

## 🙏 致谢

- [ECDICT](https://github.com/skywind3000/ECDICT) - 开源英汉词典
- [有道智云](https://ai.youdao.com/) - 词典API服务
- [Pexels](https://www.pexels.com/) - 免费图片API

---

⭐ 如果这个项目对你有帮助，请给个Star！
