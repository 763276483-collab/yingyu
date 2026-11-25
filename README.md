# 映语 (YingYu)

**映照心声，与每个灵魂对话。**

一个支持自建人物卡、接入自定义大模型 API、为每个角色独立配置声音与沉浸式液态玻璃壁纸的原生 Android AI 角色对话应用（Kotlin + Jetpack Compose，支持 Android 8.0+）。

> 当前版本：**v0.3.8**（`versionCode = 15`）｜ 开源协议：**GNU GPL-3.0**

---

## 核心特性

- **自建独立人物卡**：每张人物卡独立保存名字、简介、角色设定、开场白、**独立壁纸**、**独立头像**（默认自动裁剪壁纸人物顶部区域，也可单独自定义）以及**独立声音配置**；新建对话自动冻结角色快照。
- **沉浸式液态玻璃界面**：
  - 全屏静态壁纸铺底 + 共享低分辨率高斯模糊缓存 + 气泡/顶栏/输入栏/右上角弹出菜单实时局部采样（兼容 Android 8.0+）。
  - 底部实时预览「外观与字体」面板：通透度（52%–100%）、模糊度（0–32）、8 种预设字体色与 `#RRGGBB` 自定义字体色。
  - **陀螺仪壁纸动态视差**：自动握姿归零、后台零耗电，并与毛玻璃模糊层光学同步。
  - **沉浸聚焦模式**：默认底部对齐仅显示最近一问一答，充分露出角色立绘；点击气泡上方 `⌃ 查看前文` 原地平滑展开/收起完整对话流。
- **自然角色体验与微动效**：
  - AI 回复逐字打字机效果（标点停顿、长文提速、括号旁白柔化、点击气泡立即展开）。
  - 最新回复右侧 💡 一键生成 3 条简短回复提示，点选直接发送。
  - 长按任意气泡支持回溯继续对话、复制、朗读与删除；顶栏菜单支持回溯人物至开场白。
- **云端角色语音 + 系统 TTS**：
  - 接入 MiniMax 云端语音（内置 8 个常用中文音色预设 + `/v1/get_voice` 拉取账号音色，支持软件内调节语速、音调、音色明暗、声音强弱与情绪风格），并保留系统 TTS（支持离线音色筛选）。
  - 朗读前自动过滤括号/方括号/星号旁白，只朗读人物台词；朗读中显示跳动声波与顶部头像脉冲光环；顶栏音量图标一键静音。
- **本地优先与密钥安全**：
  - 无自建后端、无账号系统，数据通过 `AtomicFile` 原子保存在应用私有 `noBackupFilesDir`。
  - 聊天与语音 API Key 分别通过 Android Keystore + AES-GCM 加密存储，禁止系统备份与明文流量。

---

## 下载与安装

普通用户无需安装 Android Studio，直接前往本仓库的 **Releases** 页面下载最新 `YingYu-v0.3.8-debug.apk` 安装到 Android 8.0+ 手机、折叠屏或平板即可。

首次配置步骤（详见 [安装说明](docs/INSTALL.md)）：
1. 打开首页右上角 **「接口设置」**，按内置引导配置 OpenAI 兼容聊天接口（支持一键填入 DeepSeek 官方配置并通过「测试连接 / 获取模型」选择模型）与 API Key。
2. 如需云端角色语音，在同一页面下方配置 MiniMax 语音接口与独立语音 Key（不配置亦可纯文字聊天或切换系统语音）。
3. 返回首页点击 **「＋ 创建人物卡」**，填写角色名字与设定即可开始对话。

---

## 本地构建与测试

环境要求：**JDK 17** + **Android SDK 35**（仓库内置 Gradle 8.11.1 Wrapper）。

```bash
# 运行全部 JVM 单元测试并构建 Debug APK
./gradlew testDebugUnitTest assembleDebug --stacktrace
```

Windows PowerShell：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug --stacktrace
```

构建产物位于 `app/build/outputs/apk/debug/app-debug.apk`。

---

## 工程文档

| 文档 | 说明 |
| --- | --- |
| [安装说明](docs/INSTALL.md) | APK 安装、首次配置、覆盖更新与常见问题 |
| [接口故障诊断](docs/API_DIAGNOSTICS.md) | DeepSeek 400 排查、连接测试、消息兼容与错误详情 |
| [需求与验收](docs/REQUIREMENTS.md) | 需求清单与原型验收标准 |
| [架构与程序原理](docs/ARCHITECTURE.md) | 模块划分、数据流、角色提示词、存储与语音原理 |
| [数据模型](docs/DATA_MODEL.md) | 字段说明、JSON 示例与版本迁移约定 |
| [玻璃聊天界面原理](docs/GLASS_DESIGN.md) | 壁纸导入、局部高斯模糊、坐标对齐与兼容性 |
| [夜间主题与前文修复说明](docs/NIGHT_AND_HISTORY_FIX.md) | 0.3.8 暗色玻璃主题、API 引导与前文收起修复原理 |
| [语音方案调研](docs/VOICE_RESEARCH.md) | 本地离线模型与云端语音 API 调研 |
| [方案决策](docs/DECISIONS.md) | 关键工程决策记录 |
| [调试手册](docs/DEBUGGING.md) | 构建、网络、记录与语音故障排查 |
| [电脑连接手机](docs/DEVICE_DEBUGGING.md) | 真机 USB 调试工具说明 |
| [验证记录](docs/VALIDATION.md) | 单元测试、构建与真机验证记录 |
| [改动日志](docs/CHANGELOG.md) | 各版本详细改动记录 |
| [后续步骤](docs/NEXT_STEPS.md) | 当前进度与后续演进方向 |

---

## 开源协议

本项目基于 **GNU General Public License v3.0 (GPL-3.0)** 开源，详见 [LICENSE](LICENSE) 与 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
