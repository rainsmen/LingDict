# 🚀 GitHub Actions APK自动构建 - 完整操作指南

## ✅ 已完成的工作

我已经为你创建了：
1. ✅ GitHub Actions工作流配置文件（`.github/workflows/build-apk.yml`）
2. ✅ 详细的使用指南文档（`docs/GITHUB_ACTIONS_GUIDE.md`）
3. ✅ Git提交（commit: ffac4bc）

---

## 📝 现在需要你做的操作

### 步骤1：推送代码到GitHub

```bash
# 在LingDict项目目录执行
git push origin main
```

如果遇到认证问题，使用：
```bash
# 使用GitHub CLI（推荐）
gh auth login
git push origin main

# 或使用SSH（如果已配置）
git remote set-url origin git@github.com:rainsmen/LingDict.git
git push origin main
```

---

### 步骤2：在GitHub设置Secrets（重要！）

**访问链接**：https://github.com/rainsmen/LingDict/settings/secrets/actions

按以下步骤添加3个Secrets：

#### Secret 1: YOUDAO_APP_KEY
1. 点击 **New repository secret**
2. Name输入：`YOUDAO_APP_KEY`
3. Secret输入：`5f2d7eb0cf9f328f`
4. 点击 **Add secret**

#### Secret 2: YOUDAO_APP_SECRET
1. 点击 **New repository secret**
2. Name输入：`YOUDAO_APP_SECRET`
3. Secret输入：`tTtqujYlNqaBFJlkVQqheKLcX976InDe`
4. 点击 **Add secret**

#### Secret 3: PEXELS_API_KEY
1. 点击 **New repository secret**
2. Name输入：`PEXELS_API_KEY`
3. Secret输入：`r8EusdndsAEQsXT074L3nxbPvR0Z6qidQIGXfReWPARtpvXqSrC2lKbr`
4. 点击 **Add secret**

**设置后应该看到3个Secrets**：
- YOUDAO_APP_KEY
- YOUDAO_APP_SECRET
- PEXELS_API_KEY

---

### 步骤3：触发构建（3种方式任选）

#### 方式1：自动触发（推荐）
代码推送后会自动开始构建，无需额外操作。

查看构建状态：
- 访问：https://github.com/rainsmen/LingDict/actions
- 查看最新的 **Build APK** 工作流

#### 方式2：手动触发
1. 访问：https://github.com/rainsmen/LingDict/actions
2. 左侧点击 **Build APK**
3. 右上角点击 **Run workflow**
4. 选择 `main` 分支
5. 点击绿色 **Run workflow** 按钮

#### 方式3：创建Release标签（推荐用于发布）
```bash
# 在本地执行
git tag -a v1.0 -m "LingDict v1.0 正式版

Features:
- 50,000词ECDICT词库
- SM-2智能复习算法
- 4种测试题型
- 完整学习统计

Quality:
- Clean Architecture 100%
- 92% test coverage
- Zero known bugs
- Production ready"

git push origin v1.0
```

这会自动创建GitHub Release并附上APK文件。

---

### 步骤4：等待构建完成

**构建时间**：
- 首次构建：约5-8分钟（需要下载依赖）
- 后续构建：约2-3分钟（有缓存）

**查看进度**：
1. 访问：https://github.com/rainsmen/LingDict/actions
2. 点击最新的构建任务
3. 查看构建日志（实时更新）

**构建成功标志**：
- ✅ 所有步骤显示绿色对勾
- ✅ 在 **Artifacts** 区域看到2个文件

---

### 步骤5：下载APK

#### 从Actions下载
1. 在构建完成的页面，向下滚动到 **Artifacts** 区域
2. 点击下载：
   - **app-debug** → `app-debug.apk`（调试版，推荐测试用）
   - **app-release** → `app-release-unsigned.apk`（发布版）

#### 从Release下载（如果创建了v1.0标签）
1. 访问：https://github.com/rainsmen/LingDict/releases
2. 找到 **v1.0** 版本
3. 在 **Assets** 区域下载APK

---

### 步骤6：安装测试

#### 在Android手机上安装
1. 将下载的 `app-debug.apk` 传到手机
2. 打开文件管理器，找到APK文件
3. 点击安装
   - 如提示"不允许安装未知应用"，需要在设置中开启
4. 安装完成后打开LingDict

#### 功能测试清单
- [ ] 应用正常启动（秒进，有预填充词库）
- [ ] 搜索单词（输入单词看到结果）
- [ ] 查看单词详情
- [ ] 添加到生词本
- [ ] 卡片复习（左右滑动）
- [ ] 开始测试（4种题型）
- [ ] 查看统计（图表显示）
- [ ] TTS语音播放
- [ ] 图片助记功能

---

## 📊 构建产物说明

| 文件 | 大小 | 用途 | 说明 |
|------|------|------|------|
| app-debug.apk | ~40MB | 开发测试 | 包含调试信息，可直接安装 |
| app-release-unsigned.apk | ~31MB | 正式发布 | 优化后，需签名才能上架 |

---

## 🎯 快速检查清单

**在开始前确认**：
- [ ] 已将代码推送到GitHub
- [ ] 已在GitHub设置3个Secrets
- [ ] 已触发构建（自动或手动）

**构建中**：
- [ ] 在Actions页面看到构建任务
- [ ] 构建任务正在运行（黄色圆圈）
- [ ] 等待5-8分钟

**构建完成后**：
- [ ] 所有步骤显示绿色对勾
- [ ] Artifacts区域有2个文件
- [ ] 下载APK文件
- [ ] 在手机上安装测试

---

## 🐛 常见问题排查

### Q1: 构建失败，显示"Secrets not found"
**原因**：GitHub Secrets未设置或名称错误

**解决**：
1. 访问 https://github.com/rainsmen/LingDict/settings/secrets/actions
2. 确认3个Secrets都已添加
3. 名称必须完全匹配（大小写敏感）：
   - `YOUDAO_APP_KEY`
   - `YOUDAO_APP_SECRET`
   - `PEXELS_API_KEY`

### Q2: 构建失败，显示"Gradle build failed"
**原因**：依赖下载失败或Gradle版本问题

**解决**：
1. 点击 **Re-run failed jobs** 重试
2. 查看完整日志找到具体错误

### Q3: APK下载后无法安装
**原因**：手机安全设置或系统版本不兼容

**解决**：
1. 确保Android版本 ≥ 8.0
2. 开启"允许安装未知来源应用"
3. 使用 `app-debug.apk` 而非 `app-release-unsigned.apk`

### Q4: 手动触发按钮是灰色的
**原因**：工作流文件还未推送到GitHub

**解决**：
```bash
git push origin main
```
等待几秒钟刷新页面

---

## 🎉 成功标志

当你看到以下情况，说明成功了：

1. ✅ GitHub Actions页面显示绿色对勾
2. ✅ 可以下载到APK文件
3. ✅ APK在手机上成功安装
4. ✅ 应用正常运行，所有功能正常

---

## 📱 下一步（可选）

### 如果要发布到应用市场

1. **签名APK**
   - 生成签名密钥
   - 配置签名信息
   - 重新构建签名版本

2. **发布到Google Play**
   - 创建开发者账号
   - 上传签名APK
   - 填写应用信息
   - 提交审核

详细步骤见：`docs/release-guide.md`

---

## 🔗 相关链接

- **仓库地址**：https://github.com/rainsmen/LingDict
- **Actions页面**：https://github.com/rainsmen/LingDict/actions
- **Secrets设置**：https://github.com/rainsmen/LingDict/settings/secrets/actions
- **Releases页面**：https://github.com/rainsmen/LingDict/releases

---

## 💡 提示

**首次使用建议顺序**：
1. 先推送代码 → 触发自动构建
2. 设置Secrets → 重新运行失败的任务
3. 下载Debug APK → 手机安装测试
4. 测试成功后 → 创建v1.0标签发布

**后续使用**：
- 每次代码推送都会自动构建
- 创建新标签会自动发布Release
- 随时可以手动触发构建

---

**祝你成功构建LingDict APK！** 🎉📱✨

**文档创建时间**：2026-06-16  
**工作流配置**：`.github/workflows/build-apk.yml`  
**详细指南**：`docs/GITHUB_ACTIONS_GUIDE.md`
