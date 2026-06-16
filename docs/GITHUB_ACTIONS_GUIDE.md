# GitHub Actions APK自动编译配置指南

> ⚠️ **安全警告**: 本文档之前版本包含的 API 密钥已公开泄露在 Git 历史中，请勿继续使用。
> 
> **请立即采取以下行动：**
> 1. 访问 [有道智云](https://ai.youdao.com/) 和 [Pexels API](https://www.pexels.com/api/) 重新申请新的 API 密钥
> 2. 如果之前使用过泄露的密钥，请在对应平台撤销旧密钥
> 3. 使用新密钥按照下方步骤配置 GitHub Secrets

## 📋 前置要求

已创建GitHub Actions工作流配置文件：`.github/workflows/build-apk.yml`

---

## 🔑 步骤1：设置GitHub Secrets

### 访问设置页面
1. 打开你的GitHub仓库：https://github.com/rainsmen/LingDict
2. 点击 **Settings** 标签
3. 在左侧菜单找到 **Secrets and variables** → **Actions**
4. 点击 **New repository secret**

### 添加以下3个Secrets

#### Secret 1: YOUDAO_APP_KEY
- **Name**: `YOUDAO_APP_KEY`
- **Value**: `your_youdao_app_key_here`（请替换为你从有道智云申请的真实 APP Key）
- 点击 **Add secret**

#### Secret 2: YOUDAO_APP_SECRET
- **Name**: `YOUDAO_APP_SECRET`
- **Value**: `your_youdao_app_secret_here`（请替换为你从有道智云申请的真实 APP Secret）
- 点击 **Add secret**

#### Secret 3: PEXELS_API_KEY
- **Name**: `PEXELS_API_KEY`
- **Value**: `your_pexels_api_key_here`（请替换为你从 Pexels 申请的真实 API Key）
- 点击 **Add secret**

---

## 🚀 步骤2：触发构建

### 方式1：推送代码（自动触发）
```bash
git add .github/workflows/build-apk.yml
git commit -m "ci: 添加GitHub Actions自动构建APK配置"
git push origin main
```

推送后会自动开始构建。

### 方式2：手动触发
1. 访问：https://github.com/rainsmen/LingDict/actions
2. 点击左侧 **Build APK** 工作流
3. 点击右上角 **Run workflow** 按钮
4. 选择分支（main）
5. 点击绿色 **Run workflow** 按钮

### 方式3：创建Release（推荐）
```bash
# 创建v1.0标签
git tag -a v1.0 -m "LingDict v1.0正式版"
git push origin v1.0
```

创建标签后会自动构建并发布Release。

---

## 📦 步骤3：下载APK

### 从Actions下载
1. 访问：https://github.com/rainsmen/LingDict/actions
2. 点击最新的构建任务
3. 在 **Artifacts** 区域下载：
   - `app-debug.apk` - 调试版本
   - `app-release.apk` - 发布版本

### 从Release下载（如果创建了标签）
1. 访问：https://github.com/rainsmen/LingDict/releases
2. 找到对应的Release版本
3. 在 **Assets** 区域下载APK文件

---

## 🔧 工作流特性

### 触发条件
- ✅ 推送到main分支
- ✅ 创建标签（v*）
- ✅ Pull Request到main分支
- ✅ 手动触发（workflow_dispatch）

### 构建产物
- **app-debug.apk**
  - 包含调试信息
  - 可直接安装测试
  - 文件较大

- **app-release-unsigned.apk**
  - 优化后的发布版本
  - 未签名（需要签名后才能上架）
  - 文件较小

### 构建环境
- Ubuntu最新版
- JDK 17
- Gradle缓存加速
- API密钥安全注入

---

## 📱 步骤4：安装测试

### 下载后安装
1. 下载`app-debug.apk`到手机
2. 开启"未知来源"安装权限
3. 点击APK文件安装
4. 测试所有功能

### 验证清单
- [ ] 应用正常启动
- [ ] 搜索单词功能
- [ ] 添加生词功能
- [ ] 复习功能（卡片滑动）
- [ ] 测试功能（4种题型）
- [ ] 统计页面显示
- [ ] TTS语音播放
- [ ] 图片助记加载

---

## 🔐 后续：签名发布版（可选）

如果要发布到Google Play，需要签名：

### 1. 生成签名密钥
```bash
keytool -genkey -v -keystore lingdict-release.keystore \
  -alias lingdict -keyalg RSA -keysize 2048 -validity 10000
```

### 2. 添加签名配置到GitHub Secrets
- `KEYSTORE_FILE`: Base64编码的keystore文件
- `KEYSTORE_PASSWORD`: keystore密码
- `KEY_ALIAS`: 密钥别名
- `KEY_PASSWORD`: 密钥密码

### 3. 更新build.gradle
添加signingConfigs配置（详见docs/release-guide.md）

---

## ⚠️ 注意事项

### API密钥安全
- ✅ API密钥存储在GitHub Secrets中
- ✅ 不会暴露在日志中
- ✅ 只在构建时注入
- ❌ 不要把密钥提交到代码库

### 构建时间
- 首次构建：5-8分钟（下载依赖）
- 后续构建：2-3分钟（缓存加速）

### 费用
- GitHub Actions对公共仓库免费
- 每月2000分钟免费额度

---

## 🐛 常见问题

### Q: 构建失败怎么办？
A: 查看Actions日志，常见原因：
- Secrets未设置
- Gradle版本问题
- 依赖下载失败

### Q: APK无法安装？
A: 检查：
- 手机Android版本（需要8.0+）
- 是否开启未知来源安装
- 是否有存储权限

### Q: 如何更新API密钥？
A: 在GitHub Settings → Secrets中更新对应的Secret值

---

## 📚 相关文档

- [GitHub Actions官方文档](https://docs.github.com/actions)
- [发布配置指南](./release-guide.md)
- [项目完成报告](./PROJECT_COMPLETION_REPORT.md)

---

## ✅ 快速检查清单

- [ ] 创建`.github/workflows/build-apk.yml`文件
- [ ] 在GitHub仓库设置3个Secrets
- [ ] 推送代码或手动触发构建
- [ ] 等待构建完成（3-5分钟）
- [ ] 下载APK文件
- [ ] 安装到手机测试
- [ ] 验证所有功能正常

---

**完成以上步骤后，你将拥有一个自动构建的LingDict APK！** 🎉

**创建时间**: 2026-06-16  
**更新时间**: 2026-06-16
