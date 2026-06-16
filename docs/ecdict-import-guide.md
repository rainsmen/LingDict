# ECDICT词库导入指南

> 详细步骤说明  
> 日期：2026-06-16

---

## 📥 步骤1：下载ECDICT数据

### 下载源

**GitHub仓库**: https://github.com/skywind3000/ECDICT

```bash
# 方式1：直接下载CSV文件
wget https://github.com/skywind3000/ECDICT/raw/master/ecdict.csv

# 方式2：克隆整个仓库
git clone https://github.com/skywind3000/ECDICT.git
cd ECDICT
```

### 数据文件信息

- **文件名**: `ecdict.csv`
- **大小**: ~180MB（解压后）
- **词条数**: ~770,000条
- **编码**: UTF-8
- **格式**: CSV（逗号分隔）

### CSV字段说明

```
word         - 单词（唯一主键）
phonetic     - 音标
definition   - 英文释义
translation  - 中文翻译
pos          - 词性
collins      - 柯林斯星级
oxford       - 是否牛津3000核心词
tag          - 标签（CET4/CET6/考研/托福/雅思/GRE）
bnc          - BNC词频
frq          - 当代语料库词频
exchange     - 词形变化
detail       - JSON详细信息
audio        - 读音音频URL
```

---

## 🔧 步骤2：数据处理脚本

创建Python脚本处理CSV数据：

```python
# scripts/process_ecdict.py
import csv
import sqlite3
import os

def process_ecdict(csv_path, output_db_path, limit=50000):
    """
    处理ECDICT CSV文件，生成SQLite数据库
    
    Args:
        csv_path: ECDICT CSV文件路径
        output_db_path: 输出数据库路径
        limit: 导入词条数量限制（按词频排序）
    """
    
    print(f"开始处理ECDICT数据...")
    print(f"输入文件: {csv_path}")
    print(f"输出文件: {output_db_path}")
    print(f"词条限制: {limit}")
    
    # 创建数据库连接
    conn = sqlite3.connect(output_db_path)
    cursor = conn.cursor()
    
    # 创建words表（与WordEntity结构一致）
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS words (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            word TEXT NOT NULL UNIQUE,
            phonetic TEXT NOT NULL DEFAULT '',
            definition TEXT NOT NULL DEFAULT '',
            translation TEXT NOT NULL DEFAULT '',
            level TEXT,
            frequency INTEGER DEFAULT 0
        )
    ''')
    
    # 创建索引
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_word ON words(word)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_frequency ON words(frequency DESC)')
    
    # 读取CSV并排序
    print("读取CSV文件...")
    words_data = []
    
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            # 提取需要的字段
            word = row.get('word', '').strip()
            phonetic = row.get('phonetic', '').strip()
            definition = row.get('definition', '').strip()
            translation = row.get('translation', '').strip()
            tag = row.get('tag', '').strip()
            
            # 词频（BNC + frq的综合）
            bnc = int(row.get('bnc', 0) or 0)
            frq = int(row.get('frq', 0) or 0)
            frequency = bnc + frq
            
            # 确定等级
            level = determine_level(tag, row.get('oxford', ''), row.get('collins', ''))
            
            # 过滤和清洗
            if not word or len(word) > 50:
                continue
            if not translation:
                continue
                
            words_data.append({
                'word': word,
                'phonetic': phonetic,
                'definition': definition,
                'translation': translation,
                'level': level,
                'frequency': frequency
            })
    
    print(f"读取完成，共{len(words_data)}个词条")
    
    # 按词频排序
    words_data.sort(key=lambda x: x['frequency'], reverse=True)
    
    # 限制数量
    words_data = words_data[:limit]
    
    print(f"筛选后保留{len(words_data)}个词条")
    
    # 批量插入
    print("开始导入数据库...")
    batch_size = 1000
    total = len(words_data)
    
    for i in range(0, total, batch_size):
        batch = words_data[i:i+batch_size]
        cursor.executemany('''
            INSERT OR IGNORE INTO words (word, phonetic, definition, translation, level, frequency)
            VALUES (:word, :phonetic, :definition, :translation, :level, :frequency)
        ''', batch)
        
        conn.commit()
        print(f"进度: {min(i+batch_size, total)}/{total}")
    
    # 验证
    cursor.execute('SELECT COUNT(*) FROM words')
    count = cursor.fetchone()[0]
    print(f"导入完成！共{count}个词条")
    
    conn.close()
    print(f"数据库已保存到: {output_db_path}")


def determine_level(tag, oxford, collins):
    """
    根据标签确定单词等级
    """
    if not tag:
        if oxford:
            return 'OXFORD'
        if collins and int(collins or 0) >= 4:
            return 'COMMON'
        return None
    
    # 优先级：CET4 > CET6 > 考研 > 托福 > 雅思 > GRE
    if 'cet4' in tag.lower():
        return 'CET4'
    elif 'cet6' in tag.lower():
        return 'CET6'
    elif '考研' in tag or 'kaoyan' in tag.lower():
        return 'KAOYAN'
    elif 'toefl' in tag.lower() or '托福' in tag:
        return 'TOEFL'
    elif 'ielts' in tag.lower() or '雅思' in tag:
        return 'IELTS'
    elif 'gre' in tag.lower():
        return 'GRE'
    else:
        return 'COMMON'


if __name__ == '__main__':
    import sys
    
    csv_path = sys.argv[1] if len(sys.argv) > 1 else 'ecdict.csv'
    output_path = sys.argv[2] if len(sys.argv) > 2 else 'words.db'
    limit = int(sys.argv[3]) if len(sys.argv) > 3 else 50000
    
    if not os.path.exists(csv_path):
        print(f"错误：文件不存在 {csv_path}")
        sys.exit(1)
    
    process_ecdict(csv_path, output_path, limit)
```

### 运行脚本

```bash
# 安装Python（如果未安装）
# Ubuntu/Debian: sudo apt install python3
# macOS: brew install python3

# 创建scripts目录
mkdir -p scripts

# 保存上面的脚本为 scripts/process_ecdict.py

# 运行脚本（导入前5万个常用词）
python3 scripts/process_ecdict.py ecdict.csv words.db 50000

# 或导入更多词（10万）
python3 scripts/process_ecdict.py ecdict.csv words.db 100000
```

---

## 📦 步骤3：集成到Android项目

### 方式A：预填充数据库（推荐）

**优点**: 首次启动即可用，无需等待  
**缺点**: APK体积增大15-20MB

#### 1. 准备数据库文件

```bash
# 将生成的words.db复制到assets目录
mkdir -p app/src/main/assets/database
cp words.db app/src/main/assets/database/words.db
```

#### 2. 更新数据库配置

```kotlin
// app/src/main/java/com/lingdict/app/di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): LingDictDatabase {
        return Room.databaseBuilder(
            context,
            LingDictDatabase::class.java,
            LingDictDatabase.DATABASE_NAME
        )
        .createFromAsset("database/words.db") // 从assets预填充
        .addMigrations(MIGRATION_1_2)
        .build()
    }
    
    // 数据库迁移
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 版本2添加了预填充的words表
            // 表结构已在words.db中，无需额外迁移
        }
    }
}
```

#### 3. 更新数据库版本

```kotlin
// app/src/main/java/com/lingdict/app/data/local/LingDictDatabase.kt
@Database(
    entities = [
        WordEntity::class,
        UserWordEntity::class,
        ExampleEntity::class,
        StudyRecordEntity::class
    ],
    version = 2, // 从1升级到2
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LingDictDatabase : RoomDatabase() {
    // ...
}
```

---

### 方式B：首次启动导入

**优点**: APK体积小  
**缺点**: 首次启动需要等待导入（30-60秒）

#### 1. 将CSV放到assets

```bash
# 压缩CSV减小体积
gzip ecdict.csv
mv ecdict.csv.gz app/src/main/assets/ecdict.csv.gz
```

#### 2. 创建导入Service

```kotlin
// app/src/main/java/com/lingdict/app/util/DictImporter.kt
class DictImporter @Inject constructor(
    private val wordDao: WordDao
) {
    suspend fun importFromAssets(context: Context): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open("ecdict.csv.gz")
                val reader = BufferedReader(
                    InputStreamReader(
                        GZIPInputStream(inputStream),
                        Charsets.UTF_8
                    )
                )
                
                reader.readLine() // Skip header
                
                val words = mutableListOf<WordEntity>()
                var count = 0
                val limit = 50000
                
                reader.useLines { lines ->
                    lines.take(limit).forEach { line ->
                        val word = parseCsvLine(line)
                        if (word != null) {
                            words.add(word)
                            
                            if (words.size >= 1000) {
                                wordDao.insertAll(words)
                                count += words.size
                                words.clear()
                            }
                        }
                    }
                }
                
                if (words.isNotEmpty()) {
                    wordDao.insertAll(words)
                    count += words.size
                }
                
                Result.success(count)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun parseCsvLine(line: String): WordEntity? {
        // CSV解析逻辑
        // ...
    }
}
```

---

## ✅ 步骤4：验证导入

### 测试代码

```kotlin
// 测试词库是否正常
@Test
fun testWordDatabase() = runTest {
    val words = wordDao.searchWords("test", 10).first()
    assertTrue(words.isNotEmpty())
    
    val apple = wordDao.getWordByText("apple").first()
    assertNotNull(apple)
    assertEquals("apple", apple?.word)
}
```

### 手动验证

```bash
# 安装应用后，通过adb检查数据库
adb shell "run-as com.lingdict.app ls -lh databases/"

# 导出数据库查看
adb shell "run-as com.lingdict.app cat databases/lingdict.db" > lingdict.db

# 用SQLite查看
sqlite3 lingdict.db "SELECT COUNT(*) FROM words;"
sqlite3 lingdict.db "SELECT * FROM words LIMIT 10;"
```

---

## 📝 注意事项

### 1. 体积控制

- **完整词库**：77万词，~180MB，不推荐
- **常用词库**：5万词，~15MB，**推荐**
- **精简词库**：2万词，~6MB，可选

### 2. 词频排序

优先导入高频词：
- BNC词频 + 现代语料库词频
- 考试标签（CET4/CET6优先）
- 牛津3000核心词

### 3. 数据清洗

过滤掉：
- 过长单词（>50字符）
- 无中文翻译
- 专有名词
- 低频冷僻词

### 4. 版权声明

ECDICT使用MIT License，需在应用中声明：

```kotlin
// SettingsScreen.kt
Text("词库数据来源于ECDICT (MIT License)")
Text("https://github.com/skywind3000/ECDICT")
```

---

## 🚀 推荐方案总结

**最佳实践**：

1. ✅ 使用Python脚本处理ECDICT
2. ✅ 导入前5万个高频词
3. ✅ 生成SQLite数据库
4. ✅ 放到assets作为预填充数据库
5. ✅ 数据库版本升级到v2
6. ✅ 在应用中声明版权

**预计体积**: APK增加15MB左右  
**首次启动**: 无需等待，即开即用  
**用户体验**: 最佳

---

**文档版本**: v1.0  
**创建日期**: 2026-06-16  
**作者**: LingDict Dev Team
