# 验证记录

日期：2026-11-24。当前交付为 **映语 0.3.8 / versionCode 15**，已在华为 Mate 20 Pro（Android 10）同签名覆盖安装。本轮 55 项 JUnit、Android 编译、签名与 ZIP 对齐通过。普通比例 6 轮、1800×2560 宽屏尺寸模拟 3 轮前文切换通过，手机恢复到原先 1080×2340 显示覆盖值；首页与接口说明已目视/元素检查。记录见 checks/android-build-0.3.8/device-validation.md 与 NIGHT_AND_HISTORY_FIX。

本轮没有重新调用聊天生成或云端语音。此前 DeepSeek 与 MiniMax 真机成功记录保留于各版本日志，不能将本次 UI 检查当作新的生成验证；实际折叠屏和回复打字时切换仍待进一步验收。

## 历史构建环境与阶段记录

最初检查命令路径和常见 Android 安装目录时未发现可用的完整构建环境，ANDROID_HOME、JAVA_HOME 未配置。扩大检查后发现厂商软件附带的精简 Java 运行时，但没有完整 JDK 编译工具。

用户询问 APK 后，在 work/android-build 准备了便携 JDK 17、Android SDK 35、Build Tools 35.0.0、命令行工具 19.0 和 Gradle 8.11.1。未修改系统全局环境。实际编译和 JUnit 已执行并通过，仍未执行模拟器或真机测试。没有调用真实聊天/语音 API，也没有消耗用户的供应商额度。

随后用户提供 0.2.0 的聊天页截图，确认旧版界面显示和首轮 HTTP 400。未获得该响应的服务端 message 或手机实际请求，不能认定唯一原因。0.2.1 已做兼容与诊断改进，真实账号的新版模型列表和生成结果仍待用户确认；开发端未使用真实 Key。

## 本轮已做检查

| 检查 | 结果与范围 |
| --- | --- |
| Kotlin 语法解析 | 23 个 .kt 文件由 Tree-sitter Kotlin grammar 解析，无 ERROR 或缺失节点；另有实际 Kotlin/Android 编译通过 |
| Android XML | AndroidManifest.xml 与 styles.xml 均通过 PowerShell XML 解析；不等同于 Android 资源链接 |
| 工程结构与示例 | tools/verify-project.mjs 的 32 项结构检查全部通过，范围为必要文件、示例 JSON、玻璃参数范围、文档链接、HTTPS 配置与 Wrapper 完整性 |
| Gradle Wrapper JAR | SHA-256 与 services.gradle.org 官方记录相符 |
| Gradle 分发包校验配置 | 8.11.1-bin.zip 的官方 SHA-256 已固定到 wrapper 配置 |
| Android 编译 | 0.2.1 的 testDebugUnitTest assembleDebug 返回 BUILD SUCCESSFUL；增量构建 1 分 3 秒，14 项执行、27 项复用 |
| JVM 单元测试 | 7 个测试类、36 项全部通过，0 失败、0 错误、0 跳过 |
| APK 签名 | apksigner 验证通过，v2 签名，Android Debug 测试证书 |
| APK ZIP 对齐 | zipalign -c -P 16 -v 4 通过；不等于已经逐个验证所有原生库 ELF 与设备兼容性 |
| APK 元数据 | com.rolechat.app，versionCode=3，versionName=0.2.1，minSdk=26，targetSdk=35；证书与 0.2.0 相同 |
| 人工检查 | 提交取消一致性、请求显式重试、角色快照、云端独立 Key、播放器释放和语音取消路径已审阅，仍待运行验证 |

结构检查可在安装 Node.js 后执行：

```powershell
node tools/verify-project.mjs
```

Kotlin 语法检查使用临时工具 web-tree-sitter 0.20.8 与 tree-sitter-wasms 0.1.13，不是 Android 工程依赖。其结果仅用于提前发现语法问题，不能据此声称 Kotlin 编译通过。

官方校验来源：[Gradle 8.11.1 Wrapper 校验值](https://services.gradle.org/distributions/gradle-8.11.1-wrapper.jar.sha256)、[Gradle 8.11.1 分发包校验值](https://services.gradle.org/distributions/gradle-8.11.1-bin.zip.sha256)。

本次原始检查结果保存在 checks/kotlin-syntax.json、checks/project-structure.json 与 checks/xml.json，使用工程内的相对文件路径。它们是本轮静态检查证据，不是执行过的 Android 测试报告。

0.1.0 的历史静态结果为 14 个 Kotlin 文件、28 项结构检查，保存在 checks/v0.1.0。0.2.0 最初交付时同样只有静态检查；随后用户询问 APK，本轮才补齐实际编译与单元测试。没有增加未经执行的性能或视觉通过结论。

0.2.0 的首次构建/19 项 JUnit 与 APK 报告保留在 checks/android-build，静态结果保留在 checks/v0.2.0。本版的构建、36 项 JUnit、签名与对齐报告单独保存至 checks/android-build-0.2.1，不覆盖旧记录。

## 已执行的单元测试

共 7 个 JUnit 文件，36 个测试方法，全部通过：

- PromptBuilderTest：开场白转入 system 上下文、按完整用户轮次裁剪、保留待回复消息、移除历史 system、人物快照稳定性与原始记录不变，共 7 项。
- EndpointTest：地址追加与尾斜杠、拒绝 HTTP 或内嵌凭据、拒绝重复完整端点、DeepSeek 根地址/v1/models 路由，共 4 项。
- MiniMaxAudioTest：正常十六进制解码、HTTP 成功但业务失败、非法或空音频。
- GaussianBlurTest：平坦图像保持、亮点对称扩散、原缓冲不被修改、零强度与非法输入。
- AppDataCodecTest：旧版本省略字段、显式 v1、v2 往返与拒绝未知/无效版本。
- ChatClientTest：模拟传输检查请求序列化、开场白夹具、HTTP 400/402、错误脱敏、模型 GET、JSON 与文本块、非法 Key、不自动重试，共 10 项。
- ApiFailuresTest：状态分类、非 JSON 页面、长度限制与密钥脱敏、DNS 与超时，共 4 项。

这些测试不需要真实服务 Key。具备 JDK 和 SDK 后运行：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug --stacktrace
```

新增网络测试使用本地 OkHttp 应用拦截器返回固定响应，不访问真实 DeepSeek，不能当成实际 API 成功验证。当前原始构建日志、7 份 JUnit XML、签名和对齐报告保存在 checks/android-build-0.2.1。设备型号与新版真实 API 结果仍待补充。

## 可安装文件

最新交付文件：`YingYu-v0.3.7-debug.apk`（`versionCode=14`，`versionName=0.3.7`，应用名「映语」）。文件大小：11,862,778 字节（约 11.31 MiB）。

SHA-256：`a0c3f04a61b2aa73eafe96eca3b6dec2317107f98d6b8aa2ed96fc86d23864b2`。

调试签名证书 SHA-256：`d934700526c78eafc3744da7dd321a8a4167b1a00dfdcb1409fa247fcea497b4`（与 0.2.0–0.3.6 完全一致）。JUnit 11 个测试类共 **55 项全部通过**，XML 报告保存于 `docs/checks/android-build-0.3.7`；`tools/verify-project.mjs` 32/32 通过；`apksigner`（v2）与 `zipalign -c -P 16 -v 4` 通过。

## 真机验收待办（华为 Mate 20 Pro / LYA-AL00 / Android 10）

- [x] JDK/SDK 配置完成，Gradle 依赖解析与构建成功。
- [x] 全部 55 项 JUnit 通过，0.3.7 调试 APK 构建、同签名检查和 ZIP 对齐成功。
- [x] 0.3.5 真机同签名覆盖安装并启动成功（`versionCode=12`）；0.3.7 待用户在平板与折叠屏覆盖安装复验。
- [x] 0.2.1–0.2.3 真机覆盖安装、DeepSeek `GET /models`（返回 `deepseek-flash`、`deepseek-v4-pro`）及 `deepseek-flash` 单轮真实聊天在手机成功，未再出现 HTTP 400。
- [x] 人物卡创建、对话记录与加密 Key 强杀冷重启后恢复正常。
- [x] 自定义相册图片壁纸、全屏壁纸沉浸状态栏/导航栏、84% 气泡左右区分、高通透玻璃底部实时预览面板、纯黑/自定义字体颜色与聊天页「声音与自动朗读」面板在 0.2.3 真机实测通过。
- [x] MiniMax 错误码与脱敏 `status_msg` 在 0.2.2 真机可见（当前定位为 `1004: login fail: Please carry the API secret key in the 'Authorization' field of the request header`）。
- [ ] 中文长文本排版、多角色与多段会话隔离、编辑人物卡后新旧对话快照隔离。
- [ ] 密钥留空保留、更换、删除、断网/401/403/429/超时/停止与重试/撤回实机回归。
- [ ] 旋转、切后台、自定义照片壁纸选择/方向纠正/深色文字切换、长对话滚动帧率与键盘交互完整验证。
- [ ] MiniMax 正确地域/Key 配置后真实语音生成、播放、停止、防叠音与临时文件清理。
- [ ] 系统 TTS 音色、离线资源与飞行模式实测；存储异常与 v1→v2 真机迁移恢复验证。

## 当前限制

Android 编译和资源链接已经通过，后续仍可能发现供应商接口或真机运行问题。下一步先安装、体验并完成真实 API 联调，再决定流式语音与更多功能。公开发布前另外核对当前目标 SDK、签名、隐私说明、服务条款、依赖和模型授权。
