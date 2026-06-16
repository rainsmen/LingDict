# ECDICT自动导入集成说明

> 无需手动操作，首次启动自动导入  
> 日期：2026-06-16

---

## ✅ 已实现功能

### 1. 自动导入系统

**核心组件**:
- `DictionaryImporter`: 词库导入器
- `SplashViewModel`: 启动逻辑控制
- `SplashScreen`: 导入进度UI
- `RootNavigation`: 根导航管理

**工作流程**:
1. 应用启动 → 显示Splash Screen
2. 检查词库是否已导入
3. 如果未导入 → 自动从assets导入
4. 显示导入进度（进度条 + 百分比）
5. 导入完成 → 自动跳转主界面
6. 如果出错 → 显示错误信息 + 重试按钮

---

## 📥 准备词库文件

### 方式1：使用处理脚本（推荐）

```bash
# 1. 下载ECDICT原始数据
wget https://github.com/skywind3000/ECDICT/raw/master/ecdict.csv

# 2. 使用脚本处理（生成优化的CSV）
python3 scripts/prepare_ecdict_for_assets.py ecdict.csv

# 这会生成：app/src/main/assets/ecdict.csv
# 已按词频排序，仅保留前5万个常用词
```

### 方式2：手动准备

```bash
# 1. 下载ECDICT
wget https://github.com/skywind3000/ECDICT/raw/master/ecdict.csv

# 2. 提取前5万行（包含标题行）
head -n 50001 ecdict.csv > app/src/main/assets/ecdict.csv
```

---

## 🔧 代码已集成

### 已添加的文件

```
app/src/main/java/com/lingdict/app/
├── data/local/importer/
│   └── DictionaryImporter.kt          ✅ 导入器
├── presentation/
│   ├── RootNavigation.kt              ✅ 根导航
│   └── splash/
│       ├── SplashViewModel.kt         ✅ 启动逻辑
│       └── SplashScreen.kt            ✅ 启动UI
└── MainActivity.kt                    ✅ 已更新
```

### 已修改的文件

- `WordDao.kt`: 添加 `insertAll()` 方法
- `Screen.kt`: 添加 `Splash` 路由
- `MainActivity.kt`: 使用 `RootNavigation`

---

## 🎨 用户体验

### 首次启动

```
┌─────────────────────┐
│                     │
│       📚            │
│                     │
│     LingDict        │
│                     │
│  正在初始化词库...   │
│                     │
│  ▓▓▓▓▓▓▓░░░ 60%    │
│  30000 / 50000     │
│                     │
│  首次启动需要导入    │
│  请稍候，约30-60秒   │
│                     │
└─────────────────────┘
```

### 导入完成

自动进入主界面，后续启动无需等待

### 如果出错

```
┌─────────────────────┐
│                     │
│       😔            │
│                     │
│    初始化失败        │
│                     │
│  错误信息显示在这里   │
│                     │
│   ┌─────────┐       │
│   │  重 试  │       │
│   └─────────┘       │
│                     │
└─────────────────────┘
```

---

## 📊 性能优化

### 批量插入

- 每批1000条记录
- 使用事务提交
- 避免UI卡顿

### 进度反馈

- 实时进度条
- 百分比显示
- 数量显示（已导入/总数）

### 错误处理

- CSV解析错误忽略该行
- 批量插入失败会重试
- 网络无关，完全离线

---

## 🚀 使用步骤

### 1. 准备词库文件

使用提供的脚本或手动准备：

```bash
# 创建assets目录
mkdir -p app/src/main/assets

# 准备ecdict.csv文件（前5万词）
# 方式见上文
```

### 2. 编译安装

```bash
# 同步项目
./gradlew clean

# 编译安装
./gradlew installDebug
```

### 3. 首次启动

- 应用启动后自动显示Splash Screen
- 自动开始导入词库（约30-60秒）
- 导入完成自动进入主界面

### 4. 后续启动

- 检测到已有词库
- 直接进入主界面（<1秒）

---

## ⚙️ 配置选项

### 修改导入数量

编辑 `SplashViewModel.kt`:

```kotlin
// 默认5万词
dictionaryImporter.importFromAssets(context, limit = 50000)

// 改为10万词
dictionaryImporter.importFromAssets(context, limit = 100000)
```

### 修改CSV路径

编辑 `DictionaryImporter.kt`:

```kotlin
// 默认
assetPath: String = "ecdict.csv"

// 改为自定义路径
assetPath: String = "dict/my_dict.csv"
```

---

## 🐛 故障排除

### 1. CSV文件未找到

**错误**: `java.io.FileNotFoundException: ecdict.csv`

**解决**:
```bash
# 确认文件存在
ls app/src/main/assets/ecdict.csv

# 如果不存在，准备文件
mkdir -p app/src/main/assets
# 复制或生成ecdict.csv
```

### 2. 导入失败

**错误**: "导入失败: ..."

**解决**:
- 检查CSV格式是否正确
- 确认文件编码为UTF-8
- 查看logcat详细错误信息

### 3. 应用卡在Splash

**可能原因**:
- CSV文件过大
- 设备性能较低

**解决**:
- 减少导入词条数量
- 优化批量大小

---

## 📝 CSV格式要求

### 标题行

```
word,phonetic,definition,translation,pos,collins,oxford,tag,bnc,frq,exchange,detail,audio
```

### 数据行示例

```
apple,/ˈæpl/,a fruit,n. 苹果,,,,cet4,1000,500,,,
```

### 必需字段

- `word`: 单词（不能为空）
- `translation`: 中文翻译（不能为空）

### 可选字段

- `phonetic`: 音标
- `definition`: 英文释义
- `tag`: 标签（cet4/cet6等）
- `bnc`, `frq`: 词频（用于排序）

---

## 🎯 最佳实践

### 推荐配置

- **词条数量**: 50,000（约10MB）
- **批量大小**: 1,000
- **CSV编码**: UTF-8
- **文件位置**: `app/src/main/assets/ecdict.csv`

### APK体积

- 5万词: +10MB
- 10万词: +20MB
- 全量（77万词）: +180MB（不推荐）

### 导入时间

- 5万词: 30-60秒
- 10万词: 60-120秒
- 设备性能影响较大

---

## ✅ 验证导入

### 查看数据库

```bash
# 进入应用数据目录
adb shell "run-as com.lingdict.app"

# 查看数据库
cd databases
ls -lh lingdict.db

# 使用sqlite3查看
sqlite3 lingdict.db "SELECT COUNT(*) FROM words;"
sqlite3 lingdict.db "SELECT * FROM words LIMIT 10;"
```

### 应用内验证

1. 启动应用
2. 进入Home页面
3. 搜索常用单词（如"hello"）
4. 应该能看到搜索结果

---

## 🔄 更新词库

### 方式1：卸载重装

```bash
adb uninstall com.lingdict.app
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 方式2：清除数据

```bash
adb shell pm clear com.lingdict.app
```

### 方式3：代码强制重新导入

```kotlin
// 在SplashViewModel中
suspend fun forceReimport(context: Context) {
    // 清空数据库
    wordDao.deleteAll()
    
    // 重新导入
    checkAndImport(context)
}
```

---

**文档版本**: v1.0  
**更新日期**: 2026-06-16  
**状态**: 已实现，可用
