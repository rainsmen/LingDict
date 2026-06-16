# Phase 7: 词库导入与部署 - 实施计划

> 版本：v1.0  
> 日期：2026-06-16  
> 状态：准备开始

---

## 📋 概述

Phase 7是项目的最后阶段，将导入ECDICT词库数据并完成应用的最终发布准备。

**预计时间**: 1-2周  
**优先级**: 最高

---

## 🎯 任务列表

### 1. ECDICT词库准备 ⏳

**描述**: 下载和准备ECDICT词库数据

**子任务**:
- [ ] 下载ECDICT数据
  - GitHub: https://github.com/skywind3000/ECDICT
  - 格式：CSV文件，~77万词条
  - 大小：~80MB压缩，~180MB解压
  
- [ ] 数据清洗和筛选
  - 提取需要的字段（word, phonetic, definition, translation）
  - 过滤无效数据
  - 按词频排序
  - 选择常用词汇（建议：前5万词）
  
- [ ] 数据格式转换
  - CSV → JSON（便于处理）
  - 或直接生成SQL insert语句

**预计时间**: 1天

---

### 2. 数据库初始化 ⏳

**描述**: 创建预填充数据库

**子任务**:
- [ ] 创建数据库初始化脚本
  - Room database prepopulation
  - 或使用createFromAsset()
  
- [ ] 批量插入优化
  - 使用@Transaction批量插入
  - 每批1000条
  - 进度提示
  
- [ ] 索引创建
  - word字段全文索引
  - phonetic字段索引
  
- [ ] 测试导入
  - 验证数据完整性
  - 查询性能测试

**技术方案**:
```kotlin
@Database(
    entities = [WordEntity::class, UserWordEntity::class, StudyRecordEntity::class],
    version = 2, // 升级版本
    exportSchema = true
)
abstract class LingDictDatabase : RoomDatabase() {
    companion object {
        fun getInstance(context: Context): LingDictDatabase {
            return Room.databaseBuilder(
                context,
                LingDictDatabase::class.java,
                "lingdict.db"
            )
            .createFromAsset("database/words.db") // 预填充数据库
            .build()
        }
    }
}
```

**预计时间**: 2天

---

### 3. 应用签名配置 ⏳

**描述**: 配置Release签名

**子任务**:
- [ ] 生成签名密钥
  ```bash
  keytool -genkey -v -keystore lingdict.jks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias lingdict
  ```
  
- [ ] 配置gradle签名
  ```gradle
  android {
      signingConfigs {
          release {
              storeFile file("lingdict.jks")
              storePassword System.getenv("KEYSTORE_PASSWORD")
              keyAlias "lingdict"
              keyPassword System.getenv("KEY_PASSWORD")
          }
      }
      buildTypes {
          release {
              signingConfig signingConfigs.release
          }
      }
  }
  ```
  
- [ ] 配置环境变量
  - KEYSTORE_PASSWORD
  - KEY_PASSWORD
  - 添加到.gitignore

**预计时间**: 0.5天

---

### 4. ProGuard混淆 ⏳

**描述**: 配置代码混淆和优化

**子任务**:
- [ ] 启用混淆
  ```gradle
  buildTypes {
      release {
          minifyEnabled true
          shrinkResources true
          proguardFiles(
              getDefaultProguardFile("proguard-android-optimize.txt"),
              "proguard-rules.pro"
          )
      }
  }
  ```
  
- [ ] 配置ProGuard规则
  ```proguard
  # Retrofit
  -keepattributes Signature
  -keepattributes *Annotation*
  -keep class retrofit2.** { *; }
  
  # OkHttp
  -keep class okhttp3.** { *; }
  
  # Room
  -keep class * extends androidx.room.RoomDatabase
  -keep @androidx.room.Entity class *
  
  # Kotlin
  -keep class kotlin.Metadata { *; }
  
  # Data classes
  -keep class com.lingdict.app.data.remote.dto.** { *; }
  -keep class com.lingdict.app.domain.model.** { *; }
  ```
  
- [ ] 测试混淆后的APK
  - 功能验证
  - Crash测试
  - 性能测试

**预计时间**: 1天

---

### 5. 应用元数据 ⏳

**描述**: 完善应用商店信息

**子任务**:
- [ ] 应用图标
  - 1024x1024 高分辨率图标
  - Adaptive icon（前景+背景）
  - 各尺寸图标（mipmap）
  
- [ ] 应用截图
  - 至少4张截图
  - 不同功能展示
  - 手机+平板尺寸
  
- [ ] 应用描述
  - 简短描述（80字符）
  - 完整描述（4000字符）
  - 功能列表
  - 更新日志
  
- [ ] 隐私政策
  - 数据收集说明
  - 第三方服务（有道API、Pexels）
  - 托管到GitHub Pages

**预计时间**: 1天

---

### 6. Beta测试 ⏳

**描述**: 内部测试和问题修复

**子任务**:
- [ ] 创建Beta测试Track
  - Google Play Console
  - 内部测试Track
  
- [ ] 分发Beta版本
  - 生成Release APK
  - 上传到Play Console
  - 邀请测试用户
  
- [ ] 收集反馈
  - Crash报告（Firebase Crashlytics可选）
  - 用户反馈
  - Bug修复
  
- [ ] 迭代优化
  - 修复发现的问题
  - 性能优化
  - UI调整

**预计时间**: 3-5天

---

### 7. 正式发布 ⏳

**描述**: Google Play正式发布

**子任务**:
- [ ] 生产版本构建
  - versionCode = 1
  - versionName = "1.0.0"
  - Release构建
  
- [ ] Play Console配置
  - 应用分类
  - 内容分级
  - 目标用户
  - 隐私政策链接
  
- [ ] 发布审核
  - 提交审核
  - 等待批准（1-3天）
  
- [ ] 正式上线
  - 分阶段发布（10% → 50% → 100%）
  - 监控Crash率
  - 收集用户评价

**预计时间**: 2-3天

---

## 📁 数据库预填充方案

### 方案A：assets预填充（推荐）

**优点**:
- 首次启动即可用
- 无需网络下载
- 查询性能好

**缺点**:
- APK体积增大（~15MB）
- 无法动态更新

**实现**:
```kotlin
// 1. 准备words.db文件
// 2. 放到app/src/main/assets/database/
// 3. 配置Room
Room.databaseBuilder(context, LingDictDatabase::class.java, "lingdict.db")
    .createFromAsset("database/words.db")
    .build()
```

---

### 方案B：首次启动导入

**优点**:
- APK体积小
- 可动态更新词库

**缺点**:
- 首次启动慢（需导入）
- 需要等待时间

**实现**:
```kotlin
// 检查是否已导入
if (!isWordsImported()) {
    showLoadingDialog()
    viewModelScope.launch {
        importWordsFromAssets()
    }
}
```

---

### 推荐方案：方案A（assets预填充）

原因：
- 用户体验好（即开即用）
- 15MB对现代设备可接受
- 词库更新可通过应用更新

---

## 🔧 数据库迁移策略

### 版本1 → 版本2

**变更**:
- 预填充WordEntity数据

**迁移代码**:
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // WordEntity表结构未变，无需迁移
        // 数据通过createFromAsset()自动处理
    }
}

Room.databaseBuilder(context, LingDictDatabase::class.java, "lingdict.db")
    .addMigrations(MIGRATION_1_2)
    .createFromAsset("database/words.db")
    .build()
```

---

## 📊 发布检查清单

### 代码质量
- [ ] 所有测试通过
- [ ] Lint无错误
- [ ] 无TODO/FIXME
- [ ] 代码已提交

### 功能完整性
- [ ] 所有核心功能可用
- [ ] 词库数据完整
- [ ] 无明显Bug
- [ ] 性能达标

### 配置检查
- [ ] 签名配置正确
- [ ] ProGuard规则完整
- [ ] API Key已配置
- [ ] versionCode和versionName正确

### 元数据
- [ ] 应用图标完整
- [ ] 截图准备就绪
- [ ] 应用描述完成
- [ ] 隐私政策已发布

### 测试验证
- [ ] Beta测试完成
- [ ] 主要Bug已修复
- [ ] Crash率 < 1%
- [ ] 用户反馈正面

---

## 📝 版本命名规范

### versionCode
- 递增整数
- 每次发布+1
- 首次发布：1

### versionName
- 语义化版本：MAJOR.MINOR.PATCH
- 首次发布：1.0.0
- 示例：
  - 1.0.0 - 首次发布
  - 1.0.1 - Bug修复
  - 1.1.0 - 新功能
  - 2.0.0 - 重大变更

---

## ⚠️ 注意事项

### 1. 词库版权
- ECDICT使用MIT License
- 需在应用中声明
- 添加到"关于"页面

### 2. API配额
- 有道API免费额度
- Pexels API限制
- 考虑添加请求缓存

### 3. APK大小
- 目标 < 30MB
- 使用Android App Bundle
- 启用资源压缩

### 4. 性能监控
- 可选Firebase Crashlytics
- Google Play Console自带分析
- 监控ANR和Crash

---

## 🚀 时间线

### Week 1
- Day 1-2: 词库准备和数据库初始化
- Day 3: 签名配置和ProGuard
- Day 4-5: 应用元数据准备

### Week 2
- Day 6-10: Beta测试和迭代
- Day 11-12: 正式发布准备
- Day 13: 提交审核
- Day 14+: 等待上线

---

## ✅ 验收标准

### 词库完整性
- [ ] 至少5万常用词
- [ ] 数据格式正确
- [ ] 查询功能正常
- [ ] 性能可接受

### 构建质量
- [ ] Release APK可正常安装
- [ ] 签名验证通过
- [ ] 混淆后功能正常
- [ ] 体积 < 30MB

### 商店准备
- [ ] 元数据完整
- [ ] 截图符合要求
- [ ] 隐私政策可访问
- [ ] 内容分级正确

### 测试验证
- [ ] Beta测试无重大问题
- [ ] Crash率 < 1%
- [ ] 核心功能可用
- [ ] 性能达标

---

**准备就绪？让我们完成LingDict的最后阶段！** 🚀

---

**文档版本**: v1.0  
**创建日期**: 2026-06-16  
**作者**: Claude Code + LingDict Dev Team
