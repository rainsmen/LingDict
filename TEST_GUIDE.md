# 🧪 LingDict 修复验证测试指南

## 快速测试步骤

### 准备工作
1. 确保设备已连接（或模拟器已启动）
2. 确认应用已安装最新版本
3. 如果是旧版本升级，建议先卸载再安装

```bash
# 卸载旧版本
adb uninstall com.lingdict.app

# 安装新版本
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 测试1: 设置界面显示完整 ✓

**测试目的**: 验证设置界面可以滚动到底部并显示所有选项

**测试步骤**:
1. 打开应用
2. 点击底部导航栏的"设置"图标
3. 向下滚动到页面最底部
4. 检查"关于"部分的"开源许可"选项是否完整显示

**预期结果**:
- ✅ 页面可以平滑滚动
- ✅ 最底部的"开源许可"项完整显示
- ✅ 所有设置项都可以点击

**测试截图位置**: 
- 顶部：外观设置
- 中部：学习设置
- 底部：数据和关于

---

## 测试2: 学习界面发音功能 ✓

**测试目的**: 验证学习卡片上的发音按钮可以正常工作

**前置条件**:
- 生词库中至少有一个单词
- 设备音量已打开
- TTS服务已启用（首次使用可能需要下载语音包）

**测试步骤**:
1. 打开应用
2. 进入"学习"模式
3. 观察当前显示的单词卡片
4. 找到音标旁边的音量图标（扬声器图标）
5. 点击音量图标

**预期结果**:
- ✅ 点击后立即播放单词发音
- ✅ 发音清晰，语速适中
- ✅ 可以重复点击多次播放

**问题排查**:
- ❌ 如果没有声音：检查设备音量
- ❌ 如果提示"语音功能不可用"：
  - 进入设备"设置 > 语言和输入 > 文字转语音"
  - 下载英语语音数据包
  - 重启应用

---

## 测试3: 查询界面翻转卡片 ✓

**测试目的**: 验证单词详情页使用可翻转的卡片交互

**测试步骤**:
1. 打开应用
2. 在首页搜索栏输入一个单词（如 "hello"）
3. 从搜索结果中选择一个单词
4. 进入单词详情页面
5. 观察页面顶部的卡片
6. 点击卡片任意位置

**预期结果（第一次点击 - 翻转到背面）**:
- ✅ 卡片以3D动画翻转
- ✅ 背面显示：
  - "Definition"（英文释义）
  - "释义"（中文翻译）
  - 底部提示"点击返回"

**预期结果（第二次点击 - 翻转回正面）**:
- ✅ 卡片翻转回正面
- ✅ 正面显示：
  - 单词（大字体）
  - 音标
  - 音量图标（可点击发音）
  - 难度标签（如"CET4"）
  - 底部提示"点击查看释义"

**交互细节**:
- 翻转动画流畅（约600ms）
- 卡片高度固定（300dp）
- 翻转过程中有3D透视效果

---

## 测试4: 单词例句显示 ✓

**测试目的**: 验证单词详情页可以显示例句

**注意**: 此功能依赖数据库中是否有例句数据

### 测试场景A: 有例句数据

**前置条件**: 数据库中该单词有例句

**测试步骤**:
1. 查询一个有例句的单词
2. 进入单词详情页
3. 向下滚动到例句部分（在图片下方）

**预期结果**:
- ✅ 显示"例句"标题（带引号图标）
- ✅ 每个例句包含：
  - 英文句子（较大字体）
  - 中文翻译（较小字体，灰色）
  - 来源标注（如"— Oxford"，如果有的话）
- ✅ 多个例句之间有分隔线

**示例显示**:
```
例句 📝

I always keep a dictionary on my desk.
我总是在桌上放一本字典。
— Oxford

You can look up the word in the dictionary.
你可以在字典里查这个单词。
```

### 测试场景B: 无例句数据（当前状态）

**预期结果**:
- ✅ 显示"例句"标题
- ✅ 显示"暂无例句"（灰色文字）

**如何添加测试例句**:

使用adb shell连接数据库：
```bash
adb shell
cd /data/data/com.lingdict.app/databases
sqlite3 lingdict.db

# 插入测试例句
INSERT INTO examples (word, sentenceEn, sentenceZh, source) VALUES 
('hello', 'Hello, how are you?', '你好，你好吗？', 'Common'),
('hello', 'She said hello to everyone.', '她向每个人打招呼。', 'Oxford');

# 退出
.quit
```

然后重新查询该单词。

---

## 测试5: 助记图片显示 ✓

**测试目的**: 验证单词详情页可以显示助记图片

**前置条件**:
- 设备已联网
- Pexels API密钥已配置（在local.properties中）

**测试步骤**:
1. 查询一个常见单词（如 "apple", "book", "computer"）
2. 进入单词详情页
3. 观察翻转卡片下方

**预期结果**:
- ✅ 显示200dp高度的图片卡片
- ✅ 图片与单词含义相关
- ✅ 图片使用裁剪模式填充整个卡片
- ✅ 加载时有过渡动画（crossfade）

**问题排查**:
- ❌ 图片不显示：
  - 检查网络连接
  - 检查Logcat日志中的Pexels API错误
  - 验证API密钥是否有效
- ❌ 图片不相关：
  - Pexels API返回的图片基于英文关键词搜索
  - 部分抽象单词可能没有合适的图片

---

## 测试6: 音量图标在详情页发音 ✓

**测试目的**: 验证单词详情页的发音功能

**测试步骤**:
1. 查询任意单词
2. 进入单词详情页
3. 点击翻转卡片正面的音量图标（音标旁边）

**预期结果**:
- ✅ 播放单词发音
- ✅ 与学习界面的发音功能一致

---

## 完整测试流程

### 冒烟测试（5分钟）
```
1. 安装应用
2. 打开应用，检查首页正常显示
3. 搜索单词 "hello"
4. 点击进入详情页
5. 点击卡片翻转
6. 点击音量图标听发音
7. 进入设置页面，滚动到底部
8. 进入学习模式，测试发音
```

### 完整测试（15分钟）
按照上述6个测试逐一执行。

---

## 测试报告模板

```markdown
## 测试环境
- 设备: [型号/模拟器]
- Android版本: [版本号]
- 应用版本: v1.0.1
- 测试日期: [日期]

## 测试结果

| 测试项 | 状态 | 备注 |
|--------|------|------|
| 设置界面完整显示 | ✅ PASS / ❌ FAIL | |
| 学习界面发音 | ✅ PASS / ❌ FAIL | |
| 详情页翻转卡片 | ✅ PASS / ❌ FAIL | |
| 例句显示 | ✅ PASS / ❌ FAIL | |
| 助记图片 | ✅ PASS / ❌ FAIL | |
| 详情页发音 | ✅ PASS / ❌ FAIL | |

## 发现的问题
[如有问题，请详细描述]

## 截图
[附上关键界面截图]
```

---

## 性能测试

### 卡片翻转流畅度
- 翻转动画应在600ms内完成
- 无卡顿、无掉帧
- 3D旋转效果平滑

### 图片加载速度
- 首次加载：<3秒
- 缓存加载：<500ms
- 失败重试：不应阻塞UI

### 发音响应时间
- 点击到播放：<500ms
- TTS初始化：<2秒（首次）

---

## 调试命令

### 查看应用日志
```bash
adb logcat | grep LingDict
```

### 查看TTS状态
```bash
adb logcat | grep TTS
```

### 查看网络请求
```bash
adb logcat | grep -E "Pexels|Youdao"
```

### 清除应用数据（重置测试）
```bash
adb shell pm clear com.lingdict.app
```

### 查看数据库内容
```bash
adb shell
run-as com.lingdict.app
cd databases
sqlite3 lingdict.db
.tables
.schema examples
SELECT * FROM examples;
```

---

## 常见问题解决

### Q1: 发音不工作
```bash
# 检查TTS引擎
adb shell settings get secure tts_default_synth

# 如果为空，需要在设备上安装TTS引擎
```

### Q2: 图片不显示
```bash
# 检查网络
adb shell ping -c 3 api.pexels.com

# 检查API密钥
grep PEXELS_API_KEY local.properties
```

### Q3: 应用崩溃
```bash
# 查看崩溃日志
adb logcat | grep -E "AndroidRuntime|FATAL"
```

### Q4: 数据库迁移失败
```bash
# 卸载并重新安装
adb uninstall com.lingdict.app
./gradlew installDebug
```

---

## 自动化测试（可选）

如需编写Espresso UI测试，参考：

```kotlin
@Test
fun testWordDetailFlipCard() {
    // 1. 搜索单词
    onView(withId(R.id.searchBar)).perform(typeText("hello"))
    onView(withText("hello")).perform(click())
    
    // 2. 检查翻转卡片
    onView(withId(R.id.flipCard)).check(matches(isDisplayed()))
    
    // 3. 翻转卡片
    onView(withId(R.id.flipCard)).perform(click())
    
    // 4. 验证背面内容
    onView(withText("Definition")).check(matches(isDisplayed()))
}
```

---

**测试负责人**: [你的名字]  
**文档版本**: 1.0  
**最后更新**: 2026-06-17
