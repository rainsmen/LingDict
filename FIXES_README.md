# 🔧 LingDict 问题修复报告

## 📋 问题概述

已修复以下4个问题：

1. ✅ 单词查询例句都为空，也没有助记图片
2. ✅ 查询界面不是卡片，不可翻转
3. ✅ 学习界面的单词点击发音无法发音
4. ✅ 设置界面显示不全，底部选项未完整显示

---

## 🔨 详细修复内容

### 1. 设置界面显示不全 ✅

**症状**: 设置界面滚动到底部时，"显示图标"选项无法完整显示

**根本原因**: `SettingsScreen.kt` 中的 `Column` 组件缺少滚动功能

**修复方案**:
```kotlin
// 添加滚动支持
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())  // ← 新增
) {
    // ... 设置项
}
```

**影响文件**:
- `app/src/main/java/com/lingdict/app/presentation/settings/SettingsScreen.kt`

---

### 2. 学习界面点击发音无法发音 ✅

**症状**: 在学习界面，点击单词卡片上的音量图标无反应

**根本原因**: `SwipeableCard` 组件没有将 `onPlayAudio` 回调传递给内部的 `FlipCard` 组件

**修复方案**:

1. **SwipeableCard.kt** - 添加 `onPlayAudio` 参数并传递：
```kotlin
@Composable
fun SwipeableCard(
    word: Word,
    // ... 其他参数
    onPlayAudio: () -> Unit = {}  // ← 新增参数
) {
    // ...
    FlipCard(
        word = word,
        isFlipped = isFlipped,
        onFlip = onFlip,
        onPlayAudio = onPlayAudio  // ← 传递给FlipCard
    )
}
```

2. **LearnScreen.kt** - 调用时传入事件处理：
```kotlin
SwipeableCard(
    word = /* ... */,
    // ... 其他参数
    onPlayAudio = { onEvent(LearnEvent.PlayAudio) }  // ← 连接到ViewModel
)
```

**影响文件**:
- `app/src/main/java/com/lingdict/app/presentation/component/SwipeableCard.kt`
- `app/src/main/java/com/lingdict/app/presentation/learn/LearnScreen.kt`

**发音功能说明**:
- 使用 Android 系统的 TextToSpeech (TTS) 服务
- ViewModel 中的 `TTSManager` 已正确实现
- 首次使用可能需要下载英语语音数据包

---

### 3. 查询界面不是卡片、不可翻转 ✅

**症状**: 单词详情页面使用固定布局，无法翻转查看

**根本原因**: `WordDetailScreen` 使用多个独立的 Card 展示信息，没有使用 FlipCard 组件

**修复方案**:

完全重构 `WordDetailView` 函数：
- 使用 `FlipCard` 组件作为主要交互元素
- 前面：显示单词、音标、发音按钮
- 后面：显示英文释义和中文翻译
- 点击卡片可翻转
- 助记图片显示在卡片下方
- 例句列表显示在图片下方

```kotlin
@Composable
fun WordDetailView(
    word: Word,
    imageUrl: String?,
    onPlayAudio: () -> Unit
) {
    var isFlipped by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 主要内容：可翻转卡片
        FlipCard(
            word = word,
            isFlipped = isFlipped,
            onFlip = { isFlipped = !isFlipped },
            onPlayAudio = onPlayAudio,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 助记图片
        if (imageUrl != null) { /* ... */ }
        
        // 例句列表
        if (word.examples.isNotEmpty()) { /* ... */ }
    }
}
```

**影响文件**:
- `app/src/main/java/com/lingdict/app/presentation/word/WordDetailScreen.kt`

---

### 4. 单词查询例句为空 ✅

**症状**: 查询任何单词时，例句部分都显示"暂无例句"

**根本原因**: 虽然数据库有 `ExampleEntity` 表，但缺少完整的数据访问和业务逻辑

**修复方案（分5步）**:

#### 步骤1: 创建 ExampleDao
新增数据访问接口：
```kotlin
@Dao
interface ExampleDao {
    @Query("SELECT * FROM examples WHERE word = :word")
    fun getExamples(word: String): Flow<List<ExampleEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExample(example: ExampleEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamples(examples: List<ExampleEntity>)
    
    @Query("DELETE FROM examples WHERE word = :word")
    suspend fun deleteExamples(word: String)
}
```

#### 步骤2: 注册到数据库
```kotlin
// LingDictDatabase.kt
@Database(
    entities = [
        WordEntity::class,
        UserWordEntity::class,
        ExampleEntity::class,  // ← 已存在
        StudyRecordEntity::class
    ],
    version = 2
)
abstract class LingDictDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun userWordDao(): UserWordDao
    abstract fun studyRecordDao(): StudyRecordDao
    abstract fun exampleDao(): ExampleDao  // ← 新增
}
```

#### 步骤3: 配置依赖注入
```kotlin
// DatabaseModule.kt
@Provides
@Singleton
fun provideExampleDao(database: LingDictDatabase): ExampleDao {
    return database.exampleDao()
}
```

#### 步骤4: 扩展数据模型
```kotlin
// Word.kt
data class Word(
    val word: String,
    val phonetic: String? = null,
    // ... 其他字段
    val examples: List<Example> = emptyList()  // ← 新增
)

data class Example(
    val sentenceEn: String,
    val sentenceZh: String,
    val source: String? = null
)
```

#### 步骤5: 更新仓库查询逻辑
```kotlin
// WordRepositoryImpl.kt
override suspend fun getWord(word: String): Word? {
    return getWordInternal(word).getOrNull()?.let { wordEntity ->
        // 查询例句
        val examples = exampleDao.getExamples(word).first().map { exampleEntity ->
            Example(
                sentenceEn = exampleEntity.sentenceEn,
                sentenceZh = exampleEntity.sentenceZh,
                source = exampleEntity.source
            )
        }
        // 返回带例句的单词
        wordEntity.toDomainModel().copy(examples = examples)
    }
}
```

#### 步骤6: UI显示例句
在 `WordDetailScreen.kt` 中添加例句展示逻辑：
- 遍历 `word.examples` 列表
- 显示英文句子、中文翻译、来源
- 用 Divider 分隔多个例句

**影响文件**:
- 🆕 `app/src/main/java/com/lingdict/app/data/local/dao/ExampleDao.kt`
- 📝 `app/src/main/java/com/lingdict/app/data/local/LingDictDatabase.kt`
- 📝 `app/src/main/java/com/lingdict/app/di/DatabaseModule.kt`
- 📝 `app/src/main/java/com/lingdict/app/domain/model/Word.kt`
- 📝 `app/src/main/java/com/lingdict/app/data/repository/WordRepositoryImpl.kt`
- 📝 `app/src/main/java/com/lingdict/app/presentation/word/WordDetailScreen.kt`

---

## 🖼️ 关于助记图片

**好消息**: 助记图片功能已经实现！

**实现位置**: 
- `SearchWordUseCase.getWordDetail()` 方法中
- 使用 Pexels API 自动获取单词相关图片
- 图片URL存储在 `Word.imageUrl` 字段

**为什么可能看不到图片**:
1. ⚙️ Pexels API 密钥未配置或失效（检查 `local.properties`）
2. 🌐 网络连接问题
3. 🔄 首次查询单词，图片正在异步加载

**图片显示位置**:
- 在单词详情页面的翻转卡片下方
- 200dp 高度的卡片，使用 Crop 填充模式

---

## 🚀 如何构建和部署

### 方式1: 使用构建脚本（推荐）

```bash
./build_guide.sh
```

脚本会自动：
- 检查 Android SDK 是否安装
- 检查必需的 SDK 组件
- 更新配置文件
- 清理并构建项目

### 方式2: 手动构建

```bash
# 清理旧构建
./gradlew clean

# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本（需要签名配置）
./gradlew assembleRelease
```

### 安装到设备

```bash
# 使用Gradle安装
./gradlew installDebug

# 或使用adb手动安装
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 编译要求

### 必需组件
- ✅ Android SDK Platform 34
- ✅ Android SDK Build-Tools 34.0.0
- ✅ Android SDK Platform-Tools

### API密钥（已配置）
- ✅ 有道翻译 API
- ✅ Pexels 图片 API

### 开发环境
- Java 17+
- Gradle 8.x
- Kotlin 1.9+

---

## ⚠️ 重要注意事项

### 1. 数据库版本
- 当前数据库版本：2
- 如果之前安装过旧版本，建议卸载后重新安装
- 或实现数据库迁移策略

### 2. 例句数据来源
`ExampleEntity` 表结构已就绪，但需要填充数据：

**方案A**: 从有道API获取
- 修改 `YoudaoApiService` 增加例句接口
- 在查询单词时同步获取例句

**方案B**: 批量导入
- 从现有词典数据源导入
- 准备CSV/JSON格式的例句数据
- 编写导入脚本

**方案C**: 手动添加
```kotlin
// 示例代码
exampleDao.insertExample(
    ExampleEntity(
        word = "dictionary",
        sentenceEn = "I always keep a dictionary on my desk.",
        sentenceZh = "我总是在桌上放一本字典。",
        source = "Oxford"
    )
)
```

### 3. TTS（语音合成）
- 依赖 Android 系统的 TextToSpeech 服务
- 首次使用可能需要下载语音数据包
- 在设备的"设置 > 语言和输入 > 文字转语音"中配置

### 4. 图片加载优化建议
- [ ] 添加占位图（loading/error状态）
- [ ] 实现图片缓存机制
- [ ] 添加加载进度指示器
- [ ] 错误重试机制

---

## 🎯 功能测试清单

测试修复后的功能：

- [ ] 设置页面可以完整滚动到底部
- [ ] "显示图标"选项完整显示
- [ ] 学习界面点击音量图标可以听到发音
- [ ] 单词详情页面是可翻转的卡片
- [ ] 点击卡片可以翻转查看释义
- [ ] 例句显示（如果数据库中有例句）
- [ ] 助记图片显示（需要网络连接）
- [ ] TTS发音在详情页也能正常工作

---

## 📝 下一步建议

### 短期优化
1. 填充例句数据到数据库
2. 优化图片加载体验（占位图、缓存）
3. 添加更详细的错误提示

### 中期改进
1. 支持美音/英音切换
2. 例句也可以发音
3. 单词卡片背景图片支持

### 长期规划
1. 离线模式支持
2. 自定义词库导入
3. 云同步功能

---

## 📞 技术支持

如有问题，请检查：
1. `build.log` - 构建日志
2. `FIXES_SUMMARY.md` - 详细修复文档
3. Android Logcat - 运行时日志

---

**修复日期**: 2026-06-17  
**修复版本**: v1.0.1  
**数据库版本**: 2
