# LingDict 项目 Review 结论

审查日期：2026-06-16

## 总体结论

当前项目不是“可发布”状态，主分支大概率无法编译。问题主要来自模型重构后没有同步 UI、UseCase、测试、预置数据库和 CI 配置。

## 高严重度问题

### 1. 主代码存在多处编译级错误

- `app/src/main/java/com/lingdict/app/data/repository/WordRepositoryImpl.kt:152` 访问 `WordEntity` 不存在的 `pos/oxford/detail/audio` 字段。
- `app/src/main/java/com/lingdict/app/presentation/home/HomeViewModel.kt:57` 期望 `StateFlow<List<Word>>`，但 `SearchWordUseCase.invoke()` 实际返回 `Flow<List<String>>`。
- 多个 UI 文件错误导入不存在的 `com.lingdict.app.domain.model.WordStatus`，实际枚举位于 `data.local.entity.WordStatus`。
- 多个 Compose 组件直接使用 `word.level`、`word.phonetic.isNotEmpty()`、`word.translation` 等字段，但当前 `Word` 模型没有 `level`，且多数字段是 nullable。

### 2. 预置数据库与 Room schema 不兼容

- `DatabaseModule.kt` 使用 `createFromAsset("database/words.db")`。
- 实际 `words.db` 只有旧结构 `words(id, word, phonetic, definition, translation, level, frequency)`。
- 当前 Room 声明了 `words/user_words/examples/study_records` 四张实体表。
- 当前 `WordEntity` 还包含 `phoneticUs/phoneticUk/exchange/collins/bnc/frq/tag/addedTime` 等列。

结果：即使 Kotlin 编译通过，应用打开数据库时也会因为 Room schema 校验失败而启动失败。

### 3. 学习手势业务逻辑错误

- `LearnViewModel.kt:26` 左滑“不认识”默认传 `quality=2`。
- `UpdateReviewUseCase.kt:108` 把 `quality >= 2` 全部路由到 `markAsKnown()`。

结果：用户左滑“不认识”会被记录为“认识”，直接污染 SM-2 复习进度。

### 4. 测试题模型和 UI 不一致

- `TestScreen.kt` 使用 `question.prompt`、`question.fullWord`、`question.statement`。
- `Question.kt` 中没有这些字段。
- 判断题 UI 把 `correctAnswer` 当 Boolean 使用，但模型中是 String。

结果：测试模块无法编译；即使临时修编译，题目生成、作答校验和 UI 展示也需要重新对齐。

### 5. 统计模块模型不一致

- `StatisticsScreen.kt` 使用 `uiState.wordDistribution`，但 `StatisticsUiState` 没有该字段。
- `StudyRecordRepositoryImpl.kt` 构造 `StudyStatistics` 时传入当前模型不存在或不匹配的字段，例如 `totalWordsReviewed`。

结果：统计模块无法编译。

### 6. CI 和本地构建入口不可用

- 仓库存在 `gradle/wrapper/gradle-wrapper.properties`，但缺少 `gradlew` 脚本。
- README 和 GitHub Actions 都执行 `./gradlew`。
- `.github/workflows/build-apk.yml` 把 API key 写进 `gradle.properties`，但 `app/build.gradle.kts` 只读取 `local.properties`。

结果：CI 构建会失败，或构建出空 API key 的 APK。

### 7. 明文 API 密钥泄露

- `docs/GITHUB_ACTIONS_GUIDE.md:21`
- `docs/GITHUB_ACTIONS_GUIDE.md:26`
- `docs/GITHUB_ACTIONS_GUIDE.md:31`
- `docs/QUICK_START_CI.md:43`
- `docs/QUICK_START_CI.md:49`
- `docs/QUICK_START_CI.md:55`

上述位置包含真实 API key/secret。需要立即撤销或轮换这些 key，并改为占位符。

### 8. Android 资源缺失

- `AndroidManifest.xml` 引用 `@mipmap/ic_launcher` 和 `@mipmap/ic_launcher_round`。
- 仓库中没有对应 mipmap launcher 图标资源。

结果：资源合并会失败。

## 中严重度问题

### 9. 文档宣称和实际实现不一致

- README 宣称“项目完成度 100%”“可发布”“测试覆盖率 92%”。
- 实际代码无法编译，测试也与当前模型不匹配。
- README 宣称支持 PDF 导出，但隐私政策写“当前版本不支持数据导出”。
- Manifest 保留了 PDF/FileProvider/存储权限，但未看到可用的 PDF 导出实现。

### 10. Splash/词库导入路径混乱

- `MainActivity` 直接进入 `LingDictApp()`，没有走 `RootNavigation` 或 Splash。
- Splash 仍依赖 `DictionaryImporter.importFromAssets()` 导入 `ecdict.csv`。
- 当前实际数据库路径是 `createFromAsset("database/words.db")`。
- `DictionaryImporter` 仍按旧 `WordEntity(id=...)` 构造，当前实体没有 `id` 字段。

### 11. 生词详情数据链路设计不完整

- `user_words` 只保存单词字符串和复习字段。
- `UserWordRepositoryImpl.toDomainModel()` 构造的 `Word` 详情字段大多是 null。
- `GetDueWordsUseCase` 再次调用 `wordRepository.getWord()` 补详情，这会造成 N+1 查询和网络回退风险。

建议：用 DAO join 或建立明确的查询聚合模型，一次性取出用户词和词典详情。

### 12. 测试结果没有回写学习数据

- `TestViewModel.submitAnswer()` 只更新本地 UI 的 `correctCount`。
- 没有调用 `UpdateReviewUseCase.recordTestResult()`。
- 没有调用 `StudyRecordRepositoryImpl.recordTestResult()`。

结果：测试正确率、掌握状态、统计页数据不会因为测试而更新。

### 13. 设置项大多未真正生效

- 深色模式只写入 DataStore，`MainActivity` 仍使用 `LingDictTheme()` 默认系统主题。
- 每日学习目标和每日复习目标点击处理是 TODO。
- 首页进度仍硬编码 `goal = 20` 和 `goal = 30`。
- 通知开关只写入 DataStore，没有申请通知权限或调度提醒。

### 14. 错误提示被吞掉

- `HomeScreen` 遇到 `uiState.error` 后只调用 `ClearError`。
- 注释写了 “Show snackbar”，但没有实际 `SnackbarHost`。

结果：重复添加、网络失败、TTS 不可用等错误可能被立即清掉，用户看不到反馈。

### 15. 网络日志策略不适合 release

- `NetworkModule.kt` 对 OkHttp 全局启用 `HttpLoggingInterceptor.Level.BODY`。

结果：搜索词、API 响应等内容会进入日志。应只在 debug 启用，release 使用 `NONE` 或移除 interceptor。

### 16. 隐私政策与备份配置冲突

- 隐私政策强调学习数据仅本地存储。
- `backup_rules.xml` 和 `data_extraction_rules.xml` 包含 `sharedpref` 和 `database` 的云备份。

需要明确是否允许系统云备份；如果允许，隐私政策应说明；如果不允许，应调整备份规则。

## 测试现状

### 无法执行 Gradle 测试

尝试执行：

```bash
./gradlew testDebugUnitTest
```

结果：仓库缺少 `gradlew`，命令不可用。

### 测试代码本身也已落后于当前模型

- `SM2AlgorithmTest` 实例化 `SM2Algorithm()`，但当前实现是 `object`。
- `HomeViewModelTest` 和 `LearnViewModelTest` 仍按旧扁平 `UserWord` 字段构造。
- `UserWordDaoTest` 调用旧 DAO 方法，例如 `insert/getById/getWordByText`，并使用旧 `UserWordEntity` 字段。

README 中“52 个测试通过”和“92% 覆盖率”的说法当前不能成立。

## 建议修复顺序

1. 恢复可编译状态：统一 `Word/UserWord/Question/Statistics` 模型，并同步 UI、UseCase、Repository、测试。
2. 修复 Room 预置库：让 `words.db` 包含当前 Room 需要的完整 schema，或改为只预置词表并通过迁移创建用户表。
3. 补齐构建基础设施：恢复 `gradlew/gradlew.bat`，补 launcher 图标，修正 CI API key 注入。
4. 立即轮换泄露的 API key，并清理文档中的明文 secret。
5. 修复学习核心逻辑：左滑应进入 `markAsUnknown()`，测试结果应回写单词掌握度和统计。
6. 梳理功能范围：PDF、通知、Splash、设置项要么实现，要么从 README/权限/文档中移除。
7. 补回有效测试：先加编译验证，再补核心 UseCase、DAO、ViewModel 测试。

## 结论

这个仓库当前更像是多轮重构后的半同步状态：文档非常完整，但代码、资源、数据库和 CI 没有形成一个可运行闭环。下一步不建议继续增加功能，应先做一次“可构建、可启动、可搜索、可添加、可复习、可测试”的垂直收敛。
