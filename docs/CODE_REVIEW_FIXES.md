# LingDict 代码审查问题修复清单

> 日期：2026-06-16  
> 审查者：Claude Code  
> 状态：待修复

---

## 🔴 严重问题修复清单

### 问题 #1: 架构违反 - Domain层依赖Data层
**状态**: ✅ 已创建接口
**已创建**:
- `domain/repository/WordRepository.kt`
- `domain/repository/UserWordRepository.kt`
- `domain/repository/StudyRecordRepository.kt`

**待修复**:
1. 更新所有UseCase，改为依赖接口而非实现类
2. 更新Data层Repository实现类，实现这些接口
3. 更新DI配置，绑定接口到实现

**影响文件**:
```
domain/usecase/GenerateTestUseCase.kt
domain/usecase/UpdateReviewUseCase.kt
domain/usecase/AddUserWordUseCase.kt
domain/usecase/GetDueWordsUseCase.kt
domain/usecase/SearchWordUseCase.kt
domain/usecase/GetStatisticsUseCase.kt
```

---

### 问题 #2: GenerateTestUseCase缺少invoke方法
**文件**: `domain/usecase/GenerateTestUseCase.kt`
**问题**: TestViewModel调用`generateTestUseCase(type, count)`，但该方法不存在

**修复方案**:
```kotlin
operator fun invoke(type: String, count: Int): Flow<List<Question>> = flow {
    val types = listOf(type)
    val questions = generateTest(count, types)
    emit(questions)
}
```

---

### 问题 #3: UpdateReviewUseCase缺少invoke方法
**文件**: `domain/usecase/UpdateReviewUseCase.kt`
**问题**: LearnViewModel调用`updateReviewUseCase(id, quality)`，但该签名不存在

**修复方案**:
```kotlin
suspend operator fun invoke(userWordId: Long, quality: Int): Result<Unit> {
    // 1. 根据ID获取UserWord
    // 2. 使用SM-2算法计算新参数
    // 3. 更新数据库
}
```

---

### 问题 #4: Question.Listening缺少audioWord属性
**文件**: `presentation/test/TestViewModel.kt:82`
**问题**: 访问不存在的`audioWord`属性

**修复方案**:
```kotlin
// 改为
ttsManager.speak(currentQuestion.word)
```

---

### 问题 #5: Flow.collect()使用错误
**文件**: `domain/usecase/GenerateTestUseCase.kt:161-165`
**问题**: `collect()`会永久阻塞

**修复方案**:
```kotlin
val dueWords = userWordRepository.getDueWords(count).first()
```

---

### 问题 #6: Question.TrueFalse类型比较错误
**文件**: `presentation/test/TestViewModel.kt:132`
**问题**: String与Boolean比较

**修复方案**:
```kotlin
is Question.TrueFalse -> selectedAnswer == currentQuestion.correctAnswer
```

---

## 🟡 中等问题修复清单

### 问题 #7: WordDetailViewModel类型不匹配
**文件**: `presentation/word/WordDetailViewModel.kt`
**问题**: SearchWordUseCase返回类型与期望不符

**修复方案**: 调用正确的方法获取Word对象

---

### 问题 #8: PexelsRepository接口缺失
**待创建**: `domain/repository/PexelsRepository.kt`

---

### 问题 #9: GetTodayProgressUseCase返回类型错误
**文件**: `domain/usecase/GetTodayProgressUseCase.kt`
**问题**: Repository返回类型不是Flow

**修复方案**: 修改为`flow { emit(...) }`

---

### 问题 #10: StatisticsViewModel模型不匹配
**问题**: Statistics vs StudyStatistics不一致

**修复方案**: 统一使用StudyStatistics

---

### 问题 #11: SM-2算法时间溢出风险
**文件**: `domain/usecase/SM2Algorithm.kt:56`

**修复方案**:
```kotlin
val newInterval = minOf(result.interval, 365) // 限制最大365天
val nextReviewDate = System.currentTimeMillis() + newInterval * 24 * 60 * 60 * 1000L
```

---

### 问题 #12: 填空题短单词处理
**文件**: `GenerateTestUseCase.kt`

**修复方案**:
```kotlin
val hideLength = maxOf((word.length * 0.4).toInt(), 1) // 至少隐藏1个字母
```

---

### 问题 #13: getStartOfDay()时区问题
**文件**: `StudyRecordRepositoryImpl.kt`

**修复方案**:
```kotlin
import java.time.LocalDate
import java.time.ZoneId

private fun getStartOfDay(): Long {
    return LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}
```

---

## 🟢 轻微问题修复清单

### 问题 #14-22: 代码质量改进

1. **提取题型常量**:
```kotlin
object QuestionTypes {
    const val MULTIPLE_CHOICE = "choice"
    const val FILL_IN_BLANK = "fill"
    const val LISTENING = "listening"
    const val TRUE_FALSE = "judge"
}
```

2. **提取魔法数字**:
```kotlin
object ReviewThresholds {
    const val MASTERED_REPETITIONS = 5
    const val KNOWN_COUNT_THRESHOLD = 3
    const val TEST_ACCURACY_THRESHOLD = 0.75f
}
```

3. **统一模型命名**: Statistics → StudyStatistics

4. **添加空值检查**: generateMultipleChoice确保4个选项

5. **批量查询优化**: 预加载单词而非逐个查询

---

## 📋 修复优先级

### 立即修复（阻塞运行）
- ✅ #1: 创建Repository接口（已完成）
- [ ] #2: GenerateTestUseCase.invoke
- [ ] #3: UpdateReviewUseCase.invoke
- [ ] #4: Question.Listening.audioWord
- [ ] #5: Flow.collect错误
- [ ] #6: TrueFalse类型比较

### 高优先级（影响核心功能）
- [ ] #7-#10: 类型不匹配问题
- [ ] #11-#13: 潜在Bug

### 中优先级（代码质量）
- [ ] #14-#22: 代码优化

---

## 🎯 修复策略

由于问题较多且相互关联，建议采用以下策略：

1. **先修复编译问题**（#2-#6）
2. **然后修复类型系统**（#7-#10）
3. **再修复潜在Bug**（#11-#13）
4. **最后优化代码质量**（#14-#22）

**预计修复时间**: 2-3小时

---

## ⚠️ 建议

考虑到问题数量较多，建议：

1. **创建feature分支进行修复**
```bash
git checkout -b fix/code-review-issues
```

2. **分批次提交**，每修复一类问题提交一次

3. **修复后进行回归测试**，确保现有功能不受影响

4. **更新测试用例**，覆盖修复的问题

---

**文档版本**: v1.0  
**创建日期**: 2026-06-16  
**状态**: 待修复
