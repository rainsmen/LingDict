# LingDict 开发文档

> 版本：v1.0  
> 更新日期：2026-06-16  
> 目标平台：Android 10+ (API 29+), 64位

---

## 目录

1. [项目概述](#1-项目概述)
2. [开发环境配置](#2-开发环境配置)
3. [项目架构](#3-项目架构)
4. [模块设计](#4-模块设计)
5. [数据库设计](#5-数据库设计)
6. [API集成](#6-api集成)
7. [核心功能实现](#7-核心功能实现)
8. [UI组件](#8-ui组件)
9. [测试方案](#9-测试方案)
10. [构建与部署](#10-构建与部署)
11. [编码规范](#11-编码规范)

---

## 1. 项目概述

### 1.1 产品定位
LingDict 是一款基于主动查询的生词学习应用，区别于传统的词库推送式学习软件。

### 1.2 核心特性
- 🔍 智能单词查询与自动补全
- 📚 个性化生词库管理
- 🧠 SM-2算法智能复习
- 🎯 多样化测试模式
- 📊 可视化学习统计
- 🎨 精美卡片式UI设计

### 1.3 技术选型
| 技术栈 | 选型 | 版本要求 |
|--------|------|----------|
| 开发语言 | Kotlin | 1.9+ |
| UI框架 | Jetpack Compose | 1.5+ |
| 架构模式 | MVVM + Clean Architecture | - |
| 依赖注入 | Hilt | 2.48+ |
| 数据库 | Room | 2.6+ |
| 网络库 | Retrofit + OkHttp | 2.9+ / 4.11+ |
| 图片加载 | Coil | 2.5+ |
| 异步处理 | Kotlin Coroutines + Flow | 1.7+ |

---

## 2. 开发环境配置

### 2.1 必需软件
```
✅ Android Studio Ladybug (2024.1.1+) 或更高版本
✅ JDK 17
✅ Android SDK 34
✅ Git 2.30+
```

### 2.2 项目初始化
```bash
# 克隆项目
git clone https://github.com/yourusername/LingDict.git
cd LingDict

# 配置 local.properties（Android Studio自动生成）
sdk.dir=/path/to/Android/Sdk
```

### 2.3 环境变量配置
在项目根目录创建 `local.properties`：
```properties
# API Keys (不提交到Git)
YOUDAO_APP_KEY=your_youdao_app_key
YOUDAO_APP_SECRET=your_youdao_secret
PEXELS_API_KEY=your_pexels_api_key
```

### 2.4 依赖安装
```bash
# 同步Gradle依赖
./gradlew build

# 运行项目
./gradlew installDebug
```

---

## 3. 项目架构

### 3.1 整体架构图
```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│  (UI - Jetpack Compose + ViewModels)    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│           Domain Layer                   │
│  (Use Cases + Business Logic)            │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│            Data Layer                    │
│  (Repository + DataSource + Database)    │
└─────────────────────────────────────────┘
```

### 3.2 模块划分
```
app/
├── data/                  # 数据层
│   ├── local/            # 本地数据源（Room数据库）
│   ├── remote/           # 远程数据源（API）
│   └── repository/       # 仓库实现
├── domain/               # 领域层
│   ├── model/           # 领域模型
│   ├── repository/      # 仓库接口
│   └── usecase/         # 用例
├── presentation/         # 表示层
│   ├── home/            # 首页
│   ├── word/            # 单词查询
│   ├── learn/           # 学习页面
│   ├── test/            # 测试页面
│   ├── statistics/      # 统计页面
│   └── settings/        # 设置页面
└── di/                   # 依赖注入模块
```

### 3.3 包命名规范
```kotlin
com.lingdict.app
├── data
├── domain
├── presentation
├── di
└── util
```

---

## 4. 模块设计

### 4.1 数据层（Data Layer）

#### 4.1.1 本地数据源
```kotlin
// Entity定义
@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val word: String,
    val phonetic: String?,
    val definition: String,
    val translation: String,
    val level: String?, // CET4, CET6, TOEFL等
    val frequency: Int = 0
)

@Entity(tableName = "user_words")
data class UserWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val addedDate: Long,
    val lastReviewDate: Long?,
    val nextReviewDate: Long,
    val easeFactor: Float = 2.5f,
    val interval: Int = 1,
    val repetitions: Int = 0,
    val status: WordStatus // NEW, LEARNING, MASTERED
)
```

#### 4.1.2 Dao接口
```kotlin
@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE word LIKE :query || '%' LIMIT 10")
    fun searchWords(query: String): Flow<List<WordEntity>>
    
    @Query("SELECT * FROM words WHERE word = :word")
    suspend fun getWord(word: String): WordEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity)
}

@Dao
interface UserWordDao {
    @Query("SELECT * FROM user_words WHERE nextReviewDate <= :currentTime ORDER BY nextReviewDate LIMIT :limit")
    fun getDueWords(currentTime: Long, limit: Int): Flow<List<UserWordEntity>>
    
    @Query("SELECT * FROM user_words WHERE status = :status")
    fun getWordsByStatus(status: WordStatus): Flow<List<UserWordEntity>>
    
    @Update
    suspend fun updateWord(word: UserWordEntity)
    
    @Insert
    suspend fun insertWord(word: UserWordEntity)
    
    @Delete
    suspend fun deleteWord(word: UserWordEntity)
}
```

#### 4.1.3 数据库
```kotlin
@Database(
    entities = [WordEntity::class, UserWordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LingDictDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun userWordDao(): UserWordDao
}
```

### 4.2 领域层（Domain Layer）

#### 4.2.1 核心用例
```kotlin
// 搜索单词
class SearchWordUseCase @Inject constructor(
    private val repository: WordRepository
) {
    operator fun invoke(query: String): Flow<List<Word>> {
        return repository.searchWords(query)
    }
}

// 添加到生词库
class AddUserWordUseCase @Inject constructor(
    private val repository: UserWordRepository
) {
    suspend operator fun invoke(word: String): Result<Unit> {
        return repository.addUserWord(word)
    }
}

// 获取今日待复习单词
class GetDueWordsUseCase @Inject constructor(
    private val repository: UserWordRepository
) {
    operator fun invoke(limit: Int = 20): Flow<List<UserWord>> {
        return repository.getDueWords(limit)
    }
}

// 更新复习记录（SM-2算法）
class UpdateReviewUseCase @Inject constructor(
    private val repository: UserWordRepository
) {
    suspend operator fun invoke(wordId: Long, quality: Int): Result<Unit> {
        val userWord = repository.getUserWord(wordId) ?: return Result.failure(Exception("Word not found"))
        val updated = calculateNextReview(userWord, quality)
        return repository.updateUserWord(updated)
    }
    
    private fun calculateNextReview(word: UserWord, quality: Int): UserWord {
        // SM-2算法实现
        val newEaseFactor = maxOf(1.3f, word.easeFactor + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f)))
        
        val (newInterval, newRepetitions) = when {
            quality < 3 -> 1 to 0 // 重新学习
            word.repetitions == 0 -> 1 to 1
            word.repetitions == 1 -> 6 to 2
            else -> (word.interval * newEaseFactor).toInt() to word.repetitions + 1
        }
        
        return word.copy(
            easeFactor = newEaseFactor,
            interval = newInterval,
            repetitions = newRepetitions,
            lastReviewDate = System.currentTimeMillis(),
            nextReviewDate = System.currentTimeMillis() + newInterval * 24 * 60 * 60 * 1000L
        )
    }
}
```

### 4.3 表示层（Presentation Layer）

#### 4.3.1 ViewModel示例
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val searchWordUseCase: SearchWordUseCase,
    private val getDueWordsUseCase: GetDueWordsUseCase
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    val searchResults: StateFlow<List<Word>> = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.length >= 2) searchWordUseCase(query)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val dueWords: StateFlow<List<UserWord>> = getDueWordsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
```

---

## 5. 数据库设计

### 5.1 表结构

#### 5.1.1 words表（词典库）
```sql
CREATE TABLE words (
    word TEXT PRIMARY KEY,
    phonetic TEXT,
    definition TEXT NOT NULL,
    translation TEXT NOT NULL,
    level TEXT,
    frequency INTEGER DEFAULT 0
);

CREATE INDEX idx_words_prefix ON words(word);
```

#### 5.1.2 user_words表（用户生词库）
```sql
CREATE TABLE user_words (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word TEXT NOT NULL,
    added_date INTEGER NOT NULL,
    last_review_date INTEGER,
    next_review_date INTEGER NOT NULL,
    ease_factor REAL DEFAULT 2.5,
    interval INTEGER DEFAULT 1,
    repetitions INTEGER DEFAULT 0,
    status TEXT NOT NULL,
    FOREIGN KEY(word) REFERENCES words(word)
);

CREATE INDEX idx_user_words_next_review ON user_words(next_review_date);
CREATE INDEX idx_user_words_status ON user_words(status);
```

#### 5.1.3 examples表（例句库）
```sql
CREATE TABLE examples (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word TEXT NOT NULL,
    sentence_en TEXT NOT NULL,
    sentence_zh TEXT NOT NULL,
    FOREIGN KEY(word) REFERENCES words(word)
);
```

#### 5.1.4 study_records表（学习记录）
```sql
CREATE TABLE study_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date INTEGER NOT NULL,
    words_learned INTEGER DEFAULT 0,
    words_reviewed INTEGER DEFAULT 0,
    test_correct INTEGER DEFAULT 0,
    test_total INTEGER DEFAULT 0
);

CREATE INDEX idx_study_records_date ON study_records(date);
```

### 5.2 数据初始化

#### 5.2.1 ECDICT词库导入
```kotlin
class DictionaryInitializer @Inject constructor(
    private val database: LingDictDatabase
) {
    suspend fun importECDICT(inputStream: InputStream) {
        withContext(Dispatchers.IO) {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val words = mutableListOf<WordEntity>()
            
            reader.useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.split("\t")
                    if (parts.size >= 3) {
                        words.add(
                            WordEntity(
                                word = parts[0],
                                phonetic = parts.getOrNull(1),
                                definition = parts[2],
                                translation = parts.getOrNull(3) ?: "",
                                level = detectLevel(parts[0])
                            )
                        )
                        
                        if (words.size >= 1000) {
                            database.wordDao().insertAll(words)
                            words.clear()
                        }
                    }
                }
                if (words.isNotEmpty()) {
                    database.wordDao().insertAll(words)
                }
            }
        }
    }
    
    private fun detectLevel(word: String): String? {
        // 根据词频表判断等级
        return when {
            word in cet4Words -> "CET4"
            word in cet6Words -> "CET6"
            word in toeflWords -> "TOEFL"
            else -> null
        }
    }
}
```

---

## 6. API集成

### 6.1 有道词典API

#### 6.1.1 配置
```kotlin
interface YoudaoApiService {
    @GET("api")
    suspend fun translate(
        @Query("q") query: String,
        @Query("from") from: String = "en",
        @Query("to") to: String = "zh-CHS",
        @Query("appKey") appKey: String,
        @Query("salt") salt: String,
        @Query("sign") sign: String
    ): YoudaoResponse
}
```

#### 6.1.2 签名生成
```kotlin
object YoudaoSignUtil {
    fun generateSign(appKey: String, appSecret: String, query: String, salt: String): String {
        val signStr = "$appKey$query$salt$appSecret"
        return signStr.toMD5()
    }
    
    private fun String.toMD5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
```

### 6.2 Pexels图片API

#### 6.2.1 接口定义
```kotlin
interface PexelsApiService {
    @GET("v1/search")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 1,
        @Header("Authorization") apiKey: String
    ): PexelsResponse
}

data class PexelsResponse(
    val photos: List<Photo>
)

data class Photo(
    val id: Int,
    val url: String,
    val src: PhotoSrc
)

data class PhotoSrc(
    val original: String,
    val large: String,
    val medium: String,
    val small: String
)
```

### 6.3 TTS语音合成

#### 6.3.1 Google TTS集成
```kotlin
class TTSManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    
    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }
    
    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    fun stop() {
        tts?.stop()
    }
    
    fun release() {
        tts?.shutdown()
    }
}
```

---

## 7. 核心功能实现

### 7.1 单词查询与自动补全

#### 7.1.1 实现方案
```kotlin
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                expanded = it.isNotEmpty() && suggestions.isNotEmpty()
            },
            placeholder = { Text("输入单词，如 dictionary...") },
            modifier = Modifier.fillMaxWidth()
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onSuggestionClick(suggestion)
                        expanded = false
                    }
                )
            }
        }
    }
}
```

### 7.2 3D卡片翻转动画

```kotlin
@Composable
fun FlipCard(
    word: Word,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )
    
    Card(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { onFlip() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = if (rotation > 90f) 180f else 0f
                }
        ) {
            if (rotation <= 90f) {
                // 正面
                CardFront(word)
            } else {
                // 背面
                CardBack(word)
            }
        }
    }
}
```

### 7.3 滑动手势识别

```kotlin
@Composable
fun SwipeableCard(
    word: UserWord,
    onSwipeLeft: () -> Unit,  // 不认识
    onSwipeRight: () -> Unit, // 认识
    onSwipeUp: () -> Unit,    // 收藏
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    val swipeThreshold = 200f
    
    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .graphicsLayer {
                rotationZ = offsetX / 20f
                alpha = 1f - abs(offsetX) / 1000f
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            offsetX > swipeThreshold -> onSwipeRight()
                            offsetX < -swipeThreshold -> onSwipeLeft()
                            offsetY < -swipeThreshold -> onSwipeUp()
                            else -> {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            }
    ) {
        WordCard(word)
        
        // 滑动提示
        if (offsetX > 50f) {
            SwipeIndicator(text = "认识", color = Color.Green)
        } else if (offsetX < -50f) {
            SwipeIndicator(text = "不认识", color = Color.Red)
        } else if (offsetY < -50f) {
            SwipeIndicator(text = "收藏", color = Color.Blue)
        }
    }
}
```

### 7.4 测试题生成

#### 7.4.1 选择题生成
```kotlin
class MultipleChoiceGenerator @Inject constructor(
    private val wordRepository: WordRepository
) {
    suspend fun generate(correctWord: Word): Question.MultipleChoice {
        val distractors = wordRepository.getRandomWords(3, exclude = correctWord.word)
        val options = (distractors + correctWord).shuffled()
        
        return Question.MultipleChoice(
            word = correctWord.word,
            options = options.map { it.translation },
            correctAnswer = correctWord.translation
        )
    }
}
```

#### 7.4.2 填空题生成
```kotlin
class FillInBlankGenerator {
    fun generate(word: Word): Question.FillInBlank {
        val hiddenPart = word.word.substring(2, word.word.length - 1)
        val blanks = "_".repeat(hiddenPart.length)
        val displayWord = word.word.substring(0, 2) + blanks + word.word.last()
        
        return Question.FillInBlank(
            prompt = word.translation,
            displayWord = displayWord,
            correctAnswer = hiddenPart,
            fullWord = word.word
        )
    }
}
```

#### 7.4.3 听力题生成
```kotlin
class ListeningQuestionGenerator @Inject constructor(
    private val wordRepository: WordRepository,
    private val ttsManager: TTSManager
) {
    suspend fun generate(correctWord: Word): Question.Listening {
        val distractors = wordRepository.getSimilarWords(correctWord.word, 3)
        val options = (distractors + correctWord).shuffled()
        
        return Question.Listening(
            audioWord = correctWord.word,
            options = options.map { it.word },
            correctAnswer = correctWord.word,
            onPlayAudio = { ttsManager.speak(correctWord.word) }
        )
    }
}
```

### 7.5 统计图表实现

#### 7.5.1 折线图（学习趋势）
```kotlin
@Composable
fun LearningTrendChart(
    data: List<DailyRecord>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.wordsLearned } ?: 0
    
    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val barWidth = size.width / data.size
        
        data.forEachIndexed { index, record ->
            val barHeight = (record.wordsLearned.toFloat() / maxValue) * size.height
            drawRect(
                color = Color.Blue,
                topLeft = Offset(index * barWidth, size.height - barHeight),
                size = Size(barWidth * 0.8f, barHeight)
            )
        }
    }
}
```

#### 7.5.2 热力图（学习日历）
```kotlin
@Composable
fun HeatmapCalendar(
    data: Map<LocalDate, Int>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.values.maxOrNull() ?: 0
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier
    ) {
        items(90) { dayOffset ->
            val date = LocalDate.now().minusDays(dayOffset.toLong())
            val count = data[date] ?: 0
            val intensity = if (maxValue > 0) count.toFloat() / maxValue else 0f
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(2.dp)
                    .background(
                        color = Color.Green.copy(alpha = intensity),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}
```

### 7.6 PDF导出功能

```kotlin
class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun exportWords(words: List<UserWord>): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.getExternalFilesDir(null), "vocabulary_${System.currentTimeMillis()}.pdf")
            val document = PdfDocument()
            
            words.chunked(10).forEachIndexed { pageIndex, pageWords ->
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIndex + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint().apply { textSize = 12f }
                
                var yPos = 50f
                pageWords.forEach { word ->
                    canvas.drawText(word.word, 50f, yPos, paint)
                    canvas.drawText(word.translation, 200f, yPos, paint)
                    yPos += 30f
                }
                
                document.finishPage(page)
            }
            
            document.writeTo(FileOutputStream(file))
            document.close()
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 8. UI组件

### 8.1 主题系统

```kotlin
@Composable
fun LingDictTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### 8.2 通用组件

#### 8.2.1 进度环
```kotlin
@Composable
fun CircularProgress(
    current: Int,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) current.toFloat() / goal else 0f
    
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 8.dp
        )
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$current",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "/ $goal",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
```

#### 8.2.2 成就徽章
```kotlin
@Composable
fun AchievementBadge(
    achievement: Achievement,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = achievement.icon,
                contentDescription = achievement.name,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = achievement.name,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
```

---

## 9. 测试方案

### 9.1 单元测试

#### 9.1.1 ViewModel测试
```kotlin
@ExperimentalCoroutinesTest
class HomeViewModelTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var viewModel: HomeViewModel
    private lateinit var searchWordUseCase: SearchWordUseCase
    
    @Before
    fun setup() {
        searchWordUseCase = mockk()
        viewModel = HomeViewModel(searchWordUseCase, mockk())
    }
    
    @Test
    fun `search query triggers word search`() = runTest {
        // Given
        val query = "test"
        val expectedWords = listOf(Word("test", "测试"))
        coEvery { searchWordUseCase(query) } returns flowOf(expectedWords)
        
        // When
        viewModel.onSearchQueryChanged(query)
        advanceUntilIdle()
        
        // Then
        assertEquals(expectedWords, viewModel.searchResults.value)
    }
}
```

#### 9.1.2 SM-2算法测试
```kotlin
class SM2AlgorithmTest {
    
    @Test
    fun `quality 5 increases ease factor`() {
        val word = UserWord(easeFactor = 2.5f, interval = 1, repetitions = 0)
        val updated = calculateNextReview(word, quality = 5)
        
        assertTrue(updated.easeFactor > word.easeFactor)
    }
    
    @Test
    fun `quality less than 3 resets progress`() {
        val word = UserWord(easeFactor = 2.5f, interval = 10, repetitions = 5)
        val updated = calculateNextReview(word, quality = 2)
        
        assertEquals(1, updated.interval)
        assertEquals(0, updated.repetitions)
    }
    
    @Test
    fun `interval increases exponentially`() {
        var word = UserWord(easeFactor = 2.5f, interval = 1, repetitions = 0)
        
        // First review
        word = calculateNextReview(word, quality = 4)
        assertEquals(1, word.interval)
        
        // Second review
        word = calculateNextReview(word, quality = 4)
        assertEquals(6, word.interval)
        
        // Third review
        word = calculateNextReview(word, quality = 4)
        assertTrue(word.interval > 6)
    }
}
```

### 9.2 集成测试

#### 9.2.1 数据库测试
```kotlin
@RunWith(AndroidJUnit4::class)
class WordDaoTest {
    
    private lateinit var database: LingDictDatabase
    private lateinit var wordDao: WordDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LingDictDatabase::class.java
        ).build()
        wordDao = database.wordDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveWord() = runTest {
        val word = WordEntity("test", "test", "测试", "测试")
        wordDao.insertWord(word)
        
        val retrieved = wordDao.getWord("test")
        assertEquals(word, retrieved)
    }
    
    @Test
    fun searchWordsWithPrefix() = runTest {
        val words = listOf(
            WordEntity("apple", "ˈæpl", "苹果", "苹果"),
            WordEntity("application", "ˌæplɪˈkeɪʃn", "应用", "应用"),
            WordEntity("banana", "bəˈnænə", "香蕉", "香蕉")
        )
        words.forEach { wordDao.insertWord(it) }
        
        val results = wordDao.searchWords("app").first()
        assertEquals(2, results.size)
    }
}
```

### 9.3 UI测试

```kotlin
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun searchBarDisplaysCorrectly() {
        composeTestRule.setContent {
            HomeScreen()
        }
        
        composeTestRule.onNodeWithText("输入单词").assertIsDisplayed()
    }
    
    @Test
    fun searchQueryUpdatesSuggestions() {
        composeTestRule.setContent {
            HomeScreen()
        }
        
        composeTestRule.onNodeWithText("输入单词").performTextInput("dic")
        composeTestRule.onNodeWithText("dictionary").assertIsDisplayed()
    }
}
```

---

## 10. 构建与部署

### 10.1 Gradle配置

#### 10.1.1 项目级 build.gradle.kts
```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
}
```

#### 10.1.2 应用级 build.gradle.kts
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.lingdict.app"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.lingdict.app"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
```

### 10.2 GitHub Actions配置

#### 10.2.1 .github/workflows/android.yml
```yaml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

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
        cache: gradle
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Create local.properties
      run: |
        echo "YOUDAO_APP_KEY=${{ secrets.YOUDAO_APP_KEY }}" >> local.properties
        echo "YOUDAO_APP_SECRET=${{ secrets.YOUDAO_APP_SECRET }}" >> local.properties
        echo "PEXELS_API_KEY=${{ secrets.PEXELS_API_KEY }}" >> local.properties
    
    - name: Build with Gradle
      run: ./gradlew assembleDebug
    
    - name: Run tests
      run: ./gradlew test
    
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk

  release:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    needs: build
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle
    
    - name: Decode Keystore
      run: |
        echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > $GITHUB_WORKSPACE/keystore.jks
    
    - name: Build Release APK
      run: ./gradlew assembleRelease
      env:
        KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
    
    - name: Sign APK
      run: |
        $ANDROID_SDK_ROOT/build-tools/34.0.0/apksigner sign \
          --ks keystore.jks \
          --ks-pass pass:${{ secrets.KEYSTORE_PASSWORD }} \
          --key-pass pass:${{ secrets.KEY_PASSWORD }} \
          --out app/build/outputs/apk/release/app-release-signed.apk \
          app/build/outputs/apk/release/app-release-unsigned.apk
    
    - name: Create Release
      uses: actions/create-release@v1
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      with:
        tag_name: v${{ github.run_number }}
        release_name: Release v${{ github.run_number }}
        draft: false
        prerelease: false
    
    - name: Upload Release APK
      uses: actions/upload-release-asset@v1
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      with:
        upload_url: ${{ steps.create_release.outputs.upload_url }}
        asset_path: app/build/outputs/apk/release/app-release-signed.apk
        asset_name: LingDict-v${{ github.run_number }}.apk
        asset_content_type: application/vnd.android.package-archive
```

### 10.3 签名配置

#### 10.3.1 生成密钥库
```bash
keytool -genkey -v -keystore lingdict.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias lingdict
```

#### 10.3.2 Base64编码密钥库（用于GitHub Secrets）
```bash
base64 -i lingdict.jks -o keystore_base64.txt
```

#### 10.3.3 在GitHub配置Secrets
1. 进入仓库 Settings → Secrets and variables → Actions
2. 添加以下secrets：
   - `KEYSTORE_BASE64`: keystore_base64.txt的内容
   - `KEYSTORE_PASSWORD`: 密钥库密码
   - `KEY_ALIAS`: 密钥别名
   - `KEY_PASSWORD`: 密钥密码
   - `YOUDAO_APP_KEY`: 有道API Key
   - `YOUDAO_APP_SECRET`: 有道API Secret
   - `PEXELS_API_KEY`: Pexels API Key

### 10.4 .gitignore配置

```gitignore
# Built application files
*.apk
*.ap_
*.aab

# Files for the ART/Dalvik VM
*.dex

# Java class files
*.class

# Generated files
bin/
gen/
out/
build/

# Gradle files
.gradle/
gradle-app.setting

# Local configuration file (sdk path, etc)
local.properties

# IntelliJ
*.iml
.idea/
misc.xml
deploymentTargetDropDown.xml
render.experimental.xml

# Keystore files
*.jks
*.keystore

# External native build folder generated in Android Studio 2.2 and later
.externalNativeBuild
.cxx/

# OS-specific files
.DS_Store
Thumbs.db

# API Keys (important!)
secrets.properties
```

---

## 11. 编码规范

### 11.1 命名规范

#### 11.1.1 Kotlin代码规范
```kotlin
// 类名：大驼峰
class WordRepository

// 函数名：小驼峰
fun searchWords()

// 常量：大写下划线
const val MAX_SEARCH_RESULTS = 10

// 私有属性：下划线前缀
private val _searchQuery = MutableStateFlow("")

// 公共属性：小驼峰
val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
```

#### 11.1.2 资源文件命名
```
layout/         activity_main.xml, fragment_word_detail.xml
drawable/       ic_search.xml, bg_card.xml
values/         strings.xml, colors.xml, dimens.xml
```

### 11.2 代码组织

#### 11.2.1 类结构顺序
```kotlin
class ExampleClass {
    // 1. 伴生对象
    companion object {
        const val TAG = "ExampleClass"
    }
    
    // 2. 属性
    private val repository: Repository
    
    // 3. 初始化块
    init {
        // ...
    }
    
    // 4. 公共方法
    fun publicMethod() { }
    
    // 5. 私有方法
    private fun privateMethod() { }
}
```

### 11.3 注释规范

```kotlin
/**
 * 搜索符合条件的单词
 * 
 * @param query 搜索关键词，至少2个字符
 * @param limit 返回结果数量限制
 * @return 匹配的单词列表
 */
suspend fun searchWords(query: String, limit: Int = 10): List<Word> {
    require(query.length >= 2) { "查询关键词至少需要2个字符" }
    // ...
}

// TODO: 实现缓存机制
// FIXME: 处理网络异常
```

### 11.4 Compose最佳实践

```kotlin
// ✅ 推荐：状态提升
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<Word>
) {
    // UI implementation
}

// ❌ 避免：组件内部管理状态
@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }
    // ...
}

// ✅ 推荐：预览函数
@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    LingDictTheme {
        SearchScreen(
            query = "test",
            onQueryChange = {},
            searchResults = emptyList()
        )
    }
}
```

### 11.5 错误处理

```kotlin
// ✅ 使用 Result 类型
suspend fun fetchWord(word: String): Result<Word> {
    return try {
        val response = api.getWord(word)
        Result.success(response.toWord())
    } catch (e: IOException) {
        Result.failure(NetworkException("网络错误", e))
    } catch (e: Exception) {
        Result.failure(UnknownException("未知错误", e))
    }
}

// ✅ 在ViewModel中处理
viewModelScope.launch {
    fetchWord("test").fold(
        onSuccess = { word -> _wordState.value = WordState.Success(word) },
        onFailure = { error -> _wordState.value = WordState.Error(error.message) }
    )
}
```

---

## 12. 性能优化

### 12.1 数据库优化

```kotlin
// ✅ 使用索引
@Entity(
    tableName = "words",
    indices = [Index(value = ["word"])]
)

// ✅ 批量插入
@Insert
suspend fun insertAll(words: List<WordEntity>)

// ✅ 使用 Flow 避免阻塞
@Query("SELECT * FROM user_words")
fun observeUserWords(): Flow<List<UserWordEntity>>
```

### 12.2 图片加载优化

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .crossfade(true)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build(),
    contentDescription = null
)
```

### 12.3 LazyColumn优化

```kotlin
LazyColumn {
    items(
        items = words,
        key = { it.id }  // ✅ 提供稳定的key
    ) { word ->
        WordItem(word)
    }
}
```

---

## 13. 常见问题

### 13.1 问题：词库导入缓慢
**解决方案：**
```kotlin
// 使用事务批量插入
@Transaction
suspend fun importWords(words: List<WordEntity>) {
    words.chunked(1000).forEach { chunk ->
        wordDao.insertAll(chunk)
    }
}
```

### 13.2 问题：TTS无法播放
**解决方案：**
```kotlin
// 检查TTS初始化状态
if (tts?.engines?.isNotEmpty() == true) {
    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
} else {
    // 提示用户安装TTS引擎
    showTTSInstallDialog()
}
```

### 13.3 问题：GitHub Action构建失败
**解决方案：**
1. 检查Secrets配置是否正确
2. 确认Gradle版本兼容性
3. 查看构建日志中的具体错误信息

---

## 14. 开发路线图

### Phase 1: MVP (Week 1-3)
- [x] 项目搭建
- [ ] 词库集成
- [ ] 基础查询功能
- [ ] 生词库管理

### Phase 2: 核心功能 (Week 4-6)
- [ ] SM-2算法实现
- [ ] 测试模块
- [ ] 统计图表

### Phase 3: 增强体验 (Week 7-8)
- [ ] 3D卡片动画
- [ ] 图片集成
- [ ] 音频播放

### Phase 4: 完善优化 (Week 9+)
- [ ] PDF导出
- [ ] 性能优化
- [ ] Beta测试

---

## 15. 参考资源

### 15.1 官方文档
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

### 15.2 开源项目
- [ECDICT](https://github.com/skywind3000/ECDICT)
- [Tatoeba](https://tatoeba.org/zh-cn/)

### 15.3 API文档
- [有道智云API](https://ai.youdao.com/docs/)
- [Pexels API](https://www.pexels.com/api/)

---

**文档版本：** v1.0  
**最后更新：** 2026-06-16  
**维护者：** LingDict开发团队

