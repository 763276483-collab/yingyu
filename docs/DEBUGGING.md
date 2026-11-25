# 调试与增改手册

## 0.3.8 收起前文与夜间页面

收起后仍见旧气泡时，先区分展示残留与真实数据：收起只保留 latestTurnWindow 作为 UI 数据源，原会话不删除。固定入口放在 LazyColumn 外，检查模式 key 是否同时隔离列表状态与布局，不要重新把 animateItem 与全量/聚焦切换混用。31 条消息、反复展开/收起、长文、键盘与宽屏是回归重点；切换期间若新回复在打字，进度应继续。

0.3.8 首页/编辑/设置已统一夜间主题；GlassSurface 从 NightScene 共享缓存取样，避免卡片内另加不透明白底、强白边或半透明阴影。API 接入说明见 NIGHT_AND_HISTORY_FIX，官网入口不应携带 Key；国内/国际预设只修改当前输入，保存后才生效。

手机控制每次点击前检查前台应用，刚读到的控件编号不能跨页面复用。多人或多 AI 同时操作会令验证无效；本轮初次混合批次已丢弃，暂停其他操作后重新完成独立回归。详细证据见 checks/android-build-0.3.8。

## 映语图标候选与后续接入

用户已确认名称“映语”；三款原始 PNG、提示词及校验信息见同级 Yingyu-Icon-Concepts 和 BRAND_DESIGN。现在手机或 APK 显示“角色聊”不代表缓存故障，本轮尚未修改显示名称与 launcher 图标。选定图标后再检查 Manifest/资源引用、桌面圆形与圆角裁剪、小尺寸辨识度以及更新安装后的启动器缓存；保留 applicationId、签名和数据，不用卸载清数据解决品牌更新。

## 首次构建

手机接电脑进行调试时，先读 DEVICE_DEBUGGING 和工程根目录 HANDOFF：Mobile MCP 与 scrcpy 已安装，华为 Mate 20 Pro 的 ADB 已授权，0.2.1 安装成功；尚未验证应用启动和真实 API。若存在多设备，始终指定目标序列号。

本次连通关键：HDB 是 HiSuite 连接，不能代替“开发人员选项 → USB 调试”。起初只有文件传输/HDB 时 ADB 为空，真正打开 USB 调试后出现 ADB Interface 且设备为 device。现有 HDB Error 未影响 ADB，不要据此盲目更换驱动。普通沙箱调用 ADB 本地服务曾失败，提升执行成功，要区分本地服务权限与手机未识别。

Mobile MCP 已登记配置且独立 stdio 枚举真机通过；当前会话工具清单尚未热加载，不能等同于界面点击已验证。Codex 配置核对另留有 node_repl.args 差异，排查路径与加密备份位置见 HANDOFF；勿打印完整配置或整体还原旧配置。

Mobile MCP 与 uiautomator2 的常规操作无需 root；不要把缺少 USB 授权或厂商输入控制许可直接当成需要 root。先确认 adb devices 状态，再按具体机型处理输入权限。AI 自动调试优先考虑 Mobile MCP，scrcpy 辅助看屏。

需要 JDK 17、Android SDK Platform 35 与相应 Build Tools。Android Studio 可选择本工程目录并同步 Gradle，也可以使用命令行。本轮已在工作区 work/android-build 中准备便携工具，并实际完成编译与测试；用户安装 APK 不需要这些工具。

```powershell
java -version
.\gradlew.bat --version
.\gradlew.bat testDebugUnitTest assembleDebug --stacktrace
```

Wrapper 为 Gradle 8.11.1，下载包启用 SHA-256 校验。已核对 wrapper JAR 与 distribution 官方校验值。依赖无法下载时检查 Google Maven、Maven Central、Gradle 下载站的网络与代理配置，不要先盲目替换全部依赖版本。

SDK 路径由 Android Studio 自动写入 local.properties；该文件不进入版本控制。命令行环境也可以设置 ANDROID_HOME。项目不包含本机路径或真实服务 Key。

当前工作区可先执行 `. .\work\android-build\env.ps1`，再从工作区用 `outputs\RoleChatAndroid\gradlew.bat -p outputs\RoleChatAndroid testDebugUnitTest assembleDebug`。从工程根目录运行时无需 -p。环境脚本是本机工作文件，不随可移植源码 ZIP 分发；其他电脑按标准工具路径配置即可。

若下载 Gradle 超时，先确认启动脚本读取了 JAVA_OPTS/GRADLE_OPTS，代理端口可达，分发包校验保持启用。本次已把 Wrapper 超时提高至 60 秒；不把本机代理地址写进工程配置。

调试签名保存在工作区的 work/android-build/android-user/debug.keystore。后续构建应保留该文件，避免同一应用更新时出现签名冲突。不要把它放进公开源码 ZIP。手机安装方法和签名冲突处理见 INSTALL。

## 聊天不回复

| 表现 | 排查顺序 |
| --- | --- |
| 提示配置 Key | 确认填的是聊天 Key，并点击保存；语音 Key 不能替代聊天 Key |
| HTTP 401 或 403 | Key 是否有效，账号是否允许该模型，使用的地址与 Key 是否属于同一供应商 |
| HTTP 404 | 只填基础地址；程序会追加 chat/completions；供应商是否支持此协议 |
| HTTP 400 / 422 | 打开并复制新版错误详情，按服务端说明核对模型与消息参数；不能只凭状态判定 Key 错误 |
| HTTP 402 | 检查 API 账户余额；有效密钥也可能没有生成额度 |
| HTTP 429 | 检查供应商的频率或并发限制，稍后手动重试 |
| 重定向提示 | 改填官方最终接口地址，本工程不自动跟随跳转 |
| 空文本或格式错误 | 确认是文本模型，返回 choices[0].message.content；其他协议需增加适配器 |
| 长时间等待 | 检查网络、代理、模型响应速度；点击停止后可重试或撤回 |
| 上下文超限 | 减小历史轮数，缩短角色设定或消息；当前没有精确 token 预算 |
| 回复已生成但保存失败 | 保留当前数据，检查存储空间；当前版本不能恢复未落盘答案，再请求可能计费 |

不要为了调试打印 Authorization、密钥输入或完整聊天正文。记录 HTTP 状态、时间、模型 ID、应用版本和可公开的复现步骤通常已足够。

## DeepSeek HTTP 400 的本次记录

0.2.0 手机截图曾确认首轮聊天 HTTP 400，旧版未展示服务端 message。2026-11-24 已在华为 Mate 20 Pro 真机用 0.2.1/0.2.2 实测用户真实账号：`GET /models` 返回 `deepseek-flash` 与 `deepseek-v4-pro`，选择 `deepseek-flash` 后带开场白与设定发起单轮对话，成功收到中文回复，未再出现 HTTP 400。若后续在特定模型或长上下文下再次遇到 400/422，直接复制全局弹窗中的脱敏服务端说明定位。

## 云端语音不出声

1. 在“接口设置 → 云端角色语音”单独保存语音 Key，确认显示已保存状态。
2. 人物卡选择“云端语音”，voiceId 必须是账号/模型支持的 MiniMax 音色 ID；可以先在供应商控制台用同样的文本试听。
3. 确认填写的是完整的语音接口 URL，聊天基础地址规则不适用于语音字段。按账号地域核对接口：MiniMax 国内站与国际站凭据不互通，设置页 0.2.2 提供「国内站（`https://api.minimaxi.com/v1/t2a_v2`）」与「国际站（`https://api.minimax.io/v1/t2a_v2`）」快捷按钮。
4. HTTP 200 不一定代表业务成功；MiniMaxClient 检查 `base_resp.status_code` 并输出脱敏后的 `base_resp.status_msg`。例如 2026-11-24 华为真机实测 `https://api.minimax.cn/v1/t2a_v2` 返回 `1004: login fail: Please carry the API secret key in the 'Authorization' field of the request header`，常见原因为账号地域域名与 Key 不匹配、Key 为非标准付费 API Key（如套餐专用 Key）或误填了其他平台 Key。
5. 检查媒体音量、蓝牙输出和系统音频状态。本版先完整生成 MP3 再播放，长文本会有等待；当前单次限制 1500 个字符。0.3.1 起朗读前会自动过滤中英文括号/方括号/星号中的旁白与动作，只朗读说出的台词；若整句仅有旁白会自动跳过并提示。
6. 点击聊天页右上角音量图标可立即静音/停止本机朗读任务（再次点击恢复自动朗读）。已被服务商受理的请求可能继续处理和计费，不能把停止当退款。

补充：在华为 Mate 20 Pro（EMUI / Android 10）通过 Mobile MCP 调试时，元素枚举、点击、ASCII 输入和截图正常，但 `mobile_swipe_on_screen` 可能被设备拒绝 `MotionEvent`（`-32603`），滑动可回退使用 `adb -s <serial> shell input swipe`；中文输入通过 `adb input text` / `mobile_type_keys` 会被系统丢弃，自动化验证中文时可用英文提示模型回复中文，或由用户在手机输入法直接输入。另注意应用改名（如 0.2.4 改为「映语」）后首次 `adb install -r` 可能触发华为系统包安装器「风险提示 → 继续安装」弹窗，需确认后才会完成安装。

角色卡改完声音后，在“新对话”测试新音色。旧对话仍使用其角色声音快照；全局语音模型和 Key 则使用当前设置。

## 系统或本地语音不出声

- 在手机设置的“文字转语音”中确认可用引擎，安装中文音色资源，然后完全关闭并重新打开本应用刷新音色列表。
- 开启只使用离线音色而找不到音色时，应安装离线资源。不会自动降级为联网音色。
- 若使用 sherpa-onnx 引擎，先验证官方引擎 APK 本身能发声，再把系统默认 TTS 切到它，最后测试本应用的 SYSTEM 路径。
- 检查手机 CPU 架构是否与引擎 APK 一致，不要把模拟器 x86_64 版本当作手机 ARM64 版本。

## 本地数据问题

调试包可在 Android Studio Device Explorer 中查看应用的 no_backup 目录。也可以在开发设备上使用 `adb shell run-as com.rolechat.app ls no_backup` 确认文件；导出文件前注意其中包含私人对话，不能直接贴到公开问题单。

主文件为 rolechat-v1.json，AtomicFile 可能同时维护备份或新文件。损坏时先保存整个相关目录副本，再离线检查 JSON、schemaVersion 和字段；不要直接清除应用数据。未知新版本请使用支持该版本的应用打开。

语音临时文件在 cache 下以 rolechat-speech- 开头。正常播放完成或停止后删除；异常进程退出后的遗留文件可在保留主数据的前提下单独清理缓存。不要把“清除存储”与“清除缓存”混淆。

## 修改功能时的定位

| 修改 | 主要位置与同步项 |
| --- | --- |
| 新增人物字段 | Models、编辑表单、存储格式、PromptBuilder（若要发给模型）、数据文档与迁移 |
| 新增聊天协议 | 增加客户端适配器，规范统一消息输出，再接入 ViewModel；保留重试语义 |
| 新增语音供应商 | 增加独立客户端与语音路由，扩展人物声音提供商和独立配置；不要共用不兼容音色 ID |
| 流式聊天 | 增加 SSE 解析与消息状态，区分草稿、部分输出、完成和取消；不要只改 stream=true |
| 边生成边朗读 | 按句调度与音频缓冲，跟踪生成、播放、取消以及费用；需避免播放过时会话内容 |
| 备份导出 | 默认排除 Key，设置格式版本、用户选择保存位置和恢复校验 |
| 长期记忆 | 先明确记忆来源、可见与删除方式，再实现摘要或检索；不能把截断历史称为长期记忆 |

每次修复在 CHANGELOG 记录“现象 → 原因 → 修改 → 验证结果”，修改架构时同步 DECISIONS 与 ARCHITECTURE。

## 玻璃材质与聊天壁纸

聊天页“外观”为底部实时预览面板，可以选择当前角色的专属静态壁纸（不影响其他人物卡）、调整通透程度（52%–100%）与背景模糊（0–32）、选择预设或自定义 `#RRGGBB` 字体颜色。参数修改在上半屏实时预览，点击保存应用；照片选择后立即复制保存，不受取消参数修改影响。角色头像默认取当前角色壁纸顶部区域，也可在人物卡编辑页单独自定义。

| 问题 | 定位方法 |
| --- | --- |
| 气泡像实心色块 | 检查 GlassSurface 的透色浓度、高光叠层，以及输入框是否又添加不透明容器；尝试提高通透程度 |
| 文字看起来糊 | 只有背景应经过 GaussianBlur；检查是否把 blur 或位图滤镜加到了文字父层 |
| 背景图案对不上 | 检查 GlassBackdrop 的共同居中裁剪矩形、组件根坐标和壁纸原点；分别测滚动、旋转和键盘 |
| 亮壁纸看不清文字 | 切换深色文字；适当降低通透程度增加薄色层，不能假定所有图片都适合白字 |
| 选照片后没有变化 | 观察提示、文件是否超过 20 MiB、格式是否可解码；取消选择应保留原图 |
| 重启后壁纸失效 | 检查 appearance.wallpaperFile 与 no_backup/wallpapers 下文件名；不能依赖原始照片 URI |
| 滚动卡顿 | 确认每次滚动只取样共享缓存，不重新加载照片或执行卷积；用真机分析帧时间 |
| 打字机效果未触发或卡住 | 仅新收到的 AI 回复触发（历史消息不重播）；若需立即显示全文直接点击该气泡；检查 `MainViewModel.animatingMessageId` 与 `TypewriterPacing` |
| 壁纸视差未晃动或想关闭 | 检查「外观与字体」中的「壁纸动态视差」开关及设备是否具备陀螺仪/加速度计传感器；传感器仅在聊天页前台生效，切后台自动注销 |
| 旧数据升级失败 | 先保留 rolechat-v1.json 与 rolechat-v1.pre-v2.json，确认缺省版本按 v1 读取；不要删除原记录 |

照片解码在 Android 8.x 与 9+ 使用不同路径，都要验证方向和内存。更详细的计算过程见 GLASS_DESIGN。图片不会进入聊天或语音网络请求。
