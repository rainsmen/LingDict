# Phase 5: 功能增强 - 实施计划

> 版本：v1.0  
> 日期：2026-06-16  
> 状态：准备开始

---

## 📋 概述

Phase 5将在Phase 4完成的UI基础设施之上，添加以下增强功能：
- 单词详情页面
- TTS语音播放
- 图片助记功能
- 设置持久化
- 数据完善

**预计时间**: 3-5天  
**优先级**: 高

---

## 🎯 任务列表

### 1. WordDetail详情页面 ⏳

**描述**: 创建单独的单词详情展示页面，提供更丰富的信息

**子任务**:
- [ ] 创建WordDetailViewModel
  - 获取单词详细信息
  - 加载例句
  - 获取相关图片
  - 记录查看历史
  
- [ ] 创建WordDetailScreen
  - 单词卡片头部（单词、音标、发音按钮）
  - 释义部分（多义项展示）
  - 例句部分（中英对照）
  - 助记图片（Pexels API）
  - 相关单词推荐
  - 添加到生词库按钮

**技术要点**:
- 路由参数传递单词
- 图片懒加载（Coil）
- 例句列表展示
- 返回导航处理

**预计时间**: 1天

---

### 2. TTS语音播放功能 ⏳

**描述**: 集成Android TTS服务，实现单词发音功能

**子任务**:
- [ ] 创建TTSManager服务
  - 初始化TTS引擎
  - 语音播放接口
  - 生命周期管理
  
- [ ] 集成到现有页面
  - FlipCard音频按钮
  - WordDetail发音按钮
  - Test听力题音频播放
  
- [ ] 错误处理
  - TTS引擎未安装提示
  - 网络异常处理
  - 降级方案

**技术要点**:
- TextToSpeech API
- Locale.US设置
- 播放队列管理
- 资源释放

**预计时间**: 0.5天

---

### 3. Pexels图片助记功能 ⏳

**描述**: 为单词卡片和详情页添加助记图片

**子任务**:
- [ ] 完善PexelsRepository
  - 图片搜索API调用
  - 缓存策略
  - 错误处理
  
- [ ] 图片加载组件
  - Coil集成
  - 占位图显示
  - 加载失败处理
  
- [ ] 集成到UI
  - FlipCard背景图
  - WordDetail大图展示
  - 图片预加载

**技术要点**:
- Coil Compose集成
- 内存缓存优化
- 磁盘缓存策略
- 图片压缩

**预计时间**: 1天

---

### 4. 设置持久化（DataStore） ⏳

**描述**: 使用DataStore保存用户设置，替代内存状态

**子任务**:
- [ ] 创建SettingsDataStore
  - 深色模式设置
  - 通知开关
  - 每日学习目标
  - 每日复习目标
  
- [ ] 创建PreferencesRepository
  - 读写设置接口
  - Flow响应式更新
  
- [ ] 更新SettingsScreen
  - 绑定DataStore状态
  - 实时保存设置
  
- [ ] 应用全局设置
  - 主题切换响应
  - 目标值动态加载

**技术要点**:
- Preferences DataStore
- Flow + StateFlow
- 初始化时机
- 迁移策略

**预计时间**: 0.5天

---

### 5. 数据完善与优化 ⏳

**描述**: 完善统计数据查询，优化性能

**子任务**:
- [ ] 完善StudyRecordDao
  - 今日学习统计
  - 历史记录查询
  - 学习连续天数计算
  
- [ ] 优化StatisticsViewModel
  - 真实数据替换模拟数据
  - 日期范围查询
  - 缓存策略
  
- [ ] 完善HomeViewModel
  - 今日学习进度真实数据
  - 待复习单词实时更新
  
- [ ] 数据库索引优化
  - 添加必要索引
  - 查询性能分析

**技术要点**:
- Room复杂查询
- 日期时间处理
- 数据库索引
- Flow性能优化

**预计时间**: 1天

---

### 6. 用户反馈优化 ⏳

**描述**: 改善用户体验，添加反馈机制

**子任务**:
- [ ] Snackbar提示系统
  - 成功提示（添加单词、完成学习）
  - 错误提示（网络异常、操作失败）
  - 信息提示（无更多单词）
  
- [ ] 加载状态优化
  - Shimmer加载效果
  - 骨架屏
  - 下拉刷新
  
- [ ] 空状态优化
  - 更友好的空状态插图
  - 引导操作提示
  
- [ ] 错误页面
  - 网络错误页
  - 404页面
  - 通用错误处理

**技术要点**:
- SnackbarHost全局管理
- 加载状态封装
- 错误类型分类
- 重试机制

**预计时间**: 1天

---

## 📁 预计新增文件

```
presentation/
├── word/
│   ├── WordDetailViewModel.kt  ⏳ 单词详情逻辑
│   └── WordDetailScreen.kt     ⏳ 单词详情UI
├── component/
│   ├── ImageCard.kt            ⏳ 图片卡片组件
│   └── LoadingShimmer.kt       ⏳ 加载骨架屏
└── util/
    └── TTSManager.kt            ⏳ TTS管理器

data/
├── datastore/
│   └── SettingsDataStore.kt    ⏳ 设置持久化
└── repository/
    └── SettingsRepositoryImpl.kt ⏳ 设置仓库实现

domain/
├── repository/
│   └── SettingsRepository.kt   ⏳ 设置仓库接口
└── usecase/
    ├── GetSettingsUseCase.kt   ⏳ 获取设置用例
    └── UpdateSettingsUseCase.kt ⏳ 更新设置用例
```

**预计新增文件**: 约12个

---

## 🔧 技术依赖

### 已有依赖（无需添加）
- ✅ Coil (图片加载)
- ✅ DataStore (设置持久化)
- ✅ Retrofit (API调用)

### 需要验证的依赖
- ⚠️ Android TTS API（系统自带，需测试兼容性）
- ⚠️ Pexels API Key（需在local.properties中配置）

---

## 🎨 UI/UX改进

### 视觉效果
- 🎨 图片助记卡片
- 🎨 加载骨架屏动画
- 🎨 Snackbar提示样式
- 🎨 详情页布局设计

### 交互优化
- 👆 发音按钮点击反馈
- 👆 图片点击放大查看
- 👆 下拉刷新手势
- 👆 长按操作（复制单词）

---

## ⚠️ 注意事项

### 1. TTS兼容性
- 部分设备可能未安装TTS引擎
- 需要提供引导用户安装的流程
- 降级方案：显示"不支持TTS"提示

### 2. 图片加载性能
- 控制图片大小（使用Pexels的medium尺寸）
- 实现图片预加载策略
- 内存缓存限制（最多50张）

### 3. DataStore迁移
- 现有设置状态需迁移到DataStore
- 保持向后兼容
- 默认值设置

### 4. 数据库查询优化
- 添加必要的索引
- 避免主线程查询
- 分页加载大数据集

---

## 📊 性能目标

### 响应时间
- 单词搜索: < 100ms
- 图片加载: < 500ms
- 设置保存: < 50ms
- 页面导航: < 16ms (60fps)

### 内存使用
- 图片缓存: < 50MB
- 总内存占用: < 150MB

### 网络流量
- 单词查询: < 5KB
- 图片加载: < 200KB/张

---

## ✅ 验收标准

### 功能完整性
- [ ] WordDetail页面完整展示所有信息
- [ ] TTS可正常播放单词发音
- [ ] 图片助记功能正常工作
- [ ] 设置可持久化保存和读取
- [ ] 统计数据显示真实值

### 用户体验
- [ ] 所有加载状态有明确提示
- [ ] 错误信息友好易懂
- [ ] 操作反馈及时
- [ ] 页面切换流畅

### 性能表现
- [ ] 无明显卡顿
- [ ] 图片加载流畅
- [ ] 内存使用合理
- [ ] 网络流量可控

### 代码质量
- [ ] 所有新代码通过编译
- [ ] ViewModel使用Flow处理数据
- [ ] 错误处理完善
- [ ] 代码注释清晰

---

## 🔄 迭代计划

### Day 1: 核心功能（WordDetail + TTS）
- 上午：WordDetailViewModel + Screen框架
- 下午：TTS集成和测试

### Day 2: 图片功能（Pexels集成）
- 上午：PexelsRepository完善
- 下午：图片加载组件和集成

### Day 3: 数据完善（DataStore + 统计）
- 上午：SettingsDataStore实现
- 下午：统计数据查询优化

### Day 4-5: 优化与测试
- 用户反馈优化
- 性能测试
- Bug修复
- 文档更新

---

## 📝 开发指南

### 编码规范
- 遵循现有代码风格
- 使用Kotlin Flow处理异步数据
- ViewModel中不直接依赖Android API
- 所有UI组件提供Preview

### Git提交规范
```
feat: 添加单词详情页面
fix: 修复TTS播放问题
perf: 优化图片加载性能
docs: 更新Phase 5文档
```

### 分支策略
- main分支：稳定版本
- feature/phase5：Phase 5开发分支
- 完成后合并到main

---

## 🚀 开始前准备

### 环境检查
- [ ] Android Studio打开项目
- [ ] Gradle同步成功
- [ ] 模拟器或真机准备就绪
- [ ] Git状态干净

### API配置
- [ ] 验证PEXELS_API_KEY已配置
- [ ] 测试Pexels API可访问
- [ ] 确认网络权限正常

### 代码审查
- [ ] 阅读Phase 4实现代码
- [ ] 理解现有架构模式
- [ ] 熟悉Domain/Data层接口

---

**准备就绪？让我们开始Phase 5！** 🚀

---

**文档版本**: v1.0  
**创建日期**: 2026-06-16  
**作者**: Claude Code + LingDict Dev Team
