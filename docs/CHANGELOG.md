# 改动日志

### 步骤 35 发布映语 0.3.8：修复收起前文、夜间玻璃主页与 API 接入引导

2026-11-24 基于用户 0.3.7 APK 和两段录屏定位展示层问题。ChatScreen 将前文切换按钮移出消息滚动区，模式分别重建列表状态/布局，移除跨模式气泡动画，保留会话级打字机进度；收起只隐藏历史，不删消息。新增 NightGlass 的墨蓝/紫柔光背景和共享高斯缓存，全局采用暗色主题，首页人物卡使用局部玻璃、弱单描边，说明/编辑/对话框与系统栏一并适配。

ApiSettings 增加 DeepSeek 与 MiniMax 官方开放平台和文档按钮、分步接入、模型列表选择/保存说明、基础/完整 URL 区别、国内/国际站选择、语音模型快捷按钮与音色说明；国内新预设采用当前官方 api.minimax.cn。现有配置不自动迁移。涉及 MainActivity、GlassBackdrop、新增 ui/NightGlass、ui/ApiSetupGuide、app/build.gradle.kts。

版本码 15；55 项 JUnit、Android 编译、同证书签名与 ZIP 对齐检查通过。已在华为真机覆盖安装，31 条消息的对话完成 6 轮展开/收起（12 次界面检查全部通过），收起均只显示最新一问一答。首页已目视检查；宽屏与设置页记录、APK 校验信息见 checks/android-build-0.3.8。同步六份工程文档、VALIDATION、INSTALL、README 与 HANDOFF，新增 NIGHT_AND_HISTORY_FIX。本轮回归不生成付费聊天或语音，不清除应用数据。

### 步骤 34 发布「映语」0.3.7：右上角菜单实时毛玻璃化与首页卡片双层框修复

2026-11-24 按用户真机截图反馈完成 **映语 0.3.7**（`versionCode=14`，保持 `com.rolechat.app` 包名、同调试签名与 `schemaVersion=2`）：
- **聊天页右上角菜单改为实时毛玻璃**：移除不透明的 Material3 `DropdownMenu` 独立弹窗，改为聊天页同窗口 `AnimatedVisibility` 缩放淡入浮层 + `GlassSurface`，实时继承用户设置的通透度、模糊度、字体颜色与陀螺仪视差采样，点外部自动收起，菜单项均带弹簧回弹。
- **首页人物卡双层白框修复**：`SectionCard` 原半透明底色（88% 白）叠加 `shadowElevation = 2.dp` 导致系统阴影从卡片内部透出形成双层框视觉；改为纯色珠光底（`0xFFFCFBFF`）+ 清晰单层柔紫描边（`shadowElevation = 0.dp`）。
- 涉及：`MainActivity.kt`、`app/build.gradle.kts`。
- 验证结果：Gradle `testDebugUnitTest assembleDebug` 通过（11 个测试类、55 项 JUnit 全部通过），`tools/verify-project.mjs` 32/32 通过，`apksigner`（同证书 v2）与 `zipalign -c -P 16 -v 4` 通过；APK 输出为 `YingYu-v0.3.7-debug.apk`（11,862,778 字节）。

### 步骤 33 发布「映语」0.3.6：修复平板顶栏右侧按钮排版偏移与 `bounceClick` 热区缩水

2026-11-24 根据用户平板录屏反馈完成 **映语 0.3.6**（`versionCode=13`，保持 `com.rolechat.app` 包名、同调试签名与 `schemaVersion=2`）：
- **现象 → 原因 1（平板顶栏右上角按钮偏到中间）**：聊天页顶栏 `Row` 中角色名胶囊（`weight(1f, fill=false)`）与右侧 `Spacer(weight(1f))` 共同参与权重分配，在宽屏平板上各占一半剩余宽度，导致音量和菜单按钮未贴齐右边缘。
- **修改 1**：改用单个 `Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart)` 包裹角色名胶囊并移除冗余 `Spacer`，使右侧按钮在手机、折叠屏与平板上均紧贴右上角。
- **现象 → 原因 2（「收起前文」按下有回弹但偶发不触发）**：`Modifier.bounceClick` 将 `graphicsLayer` 缩放放在 `clickable` 之前，按下缩小至 94% 时边缘触点可能落出缩小后的点击范围导致 `clickable` 取消。
- **修改 2**：将 `clickable` 调整至 `graphicsLayer` 之前保持未缩放 100% 稳定触控区，并为「查看/收起前文」胶囊增加 `defaultMinSize(minHeight = 40.dp)` 与更宽松的内边距。
- 涉及：`ui/Motion.kt`、`MainActivity.kt`、`app/build.gradle.kts`。
- 验证结果：Gradle `testDebugUnitTest assembleDebug` 通过（11 个测试类、55 项 JUnit 全部通过），`tools/verify-project.mjs` 32/32 通过，`apksigner`（同证书 v2）与 `zipalign -c -P 16 -v 4` 通过；APK 输出为 `YingYu-v0.3.6-debug.apk`（11,862,778 字节），待用户在平板与折叠屏覆盖安装复验。

### 步骤 32 发布「映语」0.3.5：修复「收起前文」偶发点击失效与陀螺仪零重组优化

2026-11-24 响应用户真机反馈（「收起前文」偶尔点击无效、多点几次才响应），完成 **映语 0.3.5**（`versionCode=12`，保持 `com.rolechat.app` 包名、同调试签名与 `schemaVersion=2`）：
- **现象 → 原因**：0.3.4 在 `rememberGlassBackdrop` 组合阶段直接读取了 `rememberParallaxOffset` 状态，导致陀螺仪每 ~20ms 回调触发整个 `ChatScreen` 高频重组，干扰 `LazyColumn` 内小尺寸胶囊按钮的点击手势；同时该按钮上下内边距较小（`6.dp`）且带 `animateItem()` 位移。
- **修改**：将 `rememberParallaxOffset` 重构为 `BindParallaxToBackdrop`，由传感器回调直接写入 `GlassBackdropState.parallaxOffset`，仅在 `Canvas`/`drawBehind` 绘制阶段读取，实现陀螺仪更新期间聊天页 **0 次重组**；同时放大「查看/收起前文」胶囊点击区域并移除其 `animateItem()` 干扰。
- 涉及：`appearance/WallpaperParallax.kt`、`appearance/GlassBackdrop.kt`、`MainActivity.kt`、`app/build.gradle.kts`。
- 验证结果：Gradle `testDebugUnitTest assembleDebug` 通过（11 个测试类、55 项 JUnit 全部通过），`tools/verify-project.mjs` 32/32 通过，`apksigner`（同证书 v2）与 `zipalign -c -P 16 -v 4` 通过；APK 输出为 `YingYu-v0.3.5-debug.apk`（11,862,778 字节），已同签名覆盖安装到华为 Mate 20 Pro（`versionCode=12`）。

### 步骤 31 发布「映语」0.3.4：沉浸聚焦模式（一问一答 + 查看前文）与陀螺仪壁纸视差

2026-11-24 按用户确认方案完成 **映语 0.3.4**（`versionCode=11`，保持 `com.rolechat.app` 包名、同调试签名与 `schemaVersion=2` 向后兼容）：
- **沉浸聚焦模式（一问一答 + 一键查看前文）**：`Conversation` 新增 `latestTurnWindow()`，聊天页默认只展示最近一轮对话（最新用户发言 + 最新角色回复）并底部对齐，释放屏幕中上部充分展示角色立绘；存在更早记录时，在最新对话上方显示小巧玻璃胶囊 `⌃ 查看前文（N）`，点击即可原地平滑展开完整聊天流与顶部「简介」浮卡，再点 `⌄ 收起前文` 一键回到聚焦状态。
- **陀螺仪壁纸动态视差（与毛玻璃光学联动）**：新增 `appearance/WallpaperParallax.kt`（`ParallaxFilter` + `rememberParallaxOffset`），优先使用 `TYPE_GYROSCOPE`（缺失时自动回退加速度计），具备握姿自动归零与弹簧阻尼滤波，仅在聊天页前台可见时监听（后台 0 耗电）；`GlassBackdrop` 预放大 `1.09f` 防露边，并将视差位移同步作用于清晰壁纸与毛玻璃模糊采样层，形成真实前后景深感；「外观与字体」实时预览面板新增「壁纸动态视差」持久化开关（`ChatAppearance.parallaxEnabled`，默认开启）。
- **细节顺修**：编辑页/设置页顶栏返回按钮改用矢量 `BackArrowIcon`，避免大字体模式下文字按钮裁切。
- 涉及：`appearance/WallpaperParallax.kt`（新增）、`FocusAndParallaxTest.kt`（新增）、`appearance/GlassBackdrop.kt`、`domain/Models.kt`、`MainActivity.kt`、`app/build.gradle.kts`。
- 验证结果：Gradle `testDebugUnitTest assembleDebug` 通过（11 个测试类、55 项 JUnit 全部通过，0 失败/0 错误/0 跳过），`tools/verify-project.mjs` 32/32 通过，`apksigner`（同证书 v2）与 `zipalign -c -P 16 -v 4` 通过；APK 输出为 `YingYu-v0.3.4-debug.apk`（11,862,778 字节），已同签名覆盖安装到华为 Mate 20 Pro（`versionCode=11`）并启动验证。

### 步骤 30 发布「映语」0.3.3：AI 回复逐字打字机效果、主界面卡片动效、页面丝滑转场与聊天细节抛光

2026-11-24 按用户确认方案（打字机效果选 A + 全部 UI 动效抛光建议）完成 **映语 0.3.3**（`versionCode=10`，保持 `com.rolechat.app` 包名、同调试签名与 `schemaVersion=2`）：
- **AI 回复自然逐字打字机效果（方案 A）**：新增 `ui/Typewriter.kt`（`TypewriterPacing`）与 `MainViewModel.animatingMessageId`；仅对新收到的 AI 回复播放逐字显示（不重播历史记录），支持标点短停顿、长回复自动提速、括号旁白略快于台词，并与自动朗读同步开始；点击正在打字的气泡可立即展开全文，同时优化 `formatRoleplayText` 使其在未闭合括号打字过程中即保持旁白柔化透明度。
- **主界面动效与页面转场**：首页人物卡支持交错淡入上浮（`StaggeredEntrance`）、列表项平滑重排与删除过渡（`Modifier.animateItem()`）、整张卡片可点击进入对话并带轻弹反馈；「＋ 创建人物卡」新增流动紫蓝渐变光效（`rememberShimmerAccentBrush`）；空状态展示居中品牌图标；`RoleChatApp` 路由切换改用 `AnimatedContent` 丝滑滑动淡入淡出。
- **聊天页沉浸式细节抛光**：进入对话时壁纸带柔和淡入微缩放；等待 AI 回复时在角色气泡侧展示三点弹跳玻璃气泡（`TypingDotsIndicator`）与停止胶囊；朗读时顶部角色头像亮起柔和脉冲光环（`SpeakingAvatarAura`）；3 条 💡 建议卡片交错弹出；发送、长按菜单、点选建议与删除操作接入触感反馈（`LocalHapticFeedback`）。
- 涉及：`ui/Typewriter.kt`（新增）、`TypewriterPacingTest.kt`（新增）、`ui/Motion.kt`、`MainViewModel.kt`、`MainActivity.kt`、`app/build.gradle.kts`。
- 验证结果：Gradle `testDebugUnitTest assembleDebug` 通过（10 个测试类、51 项 JUnit 全部通过，0 失败/0 错误/0 跳过），`tools/verify-project.mjs` 32/32 通过，`apksigner`（同证书 v2）与 `zipalign -c -P 16 -v 4` 通过；APK 输出为 `YingYu-v0.3.3-debug.apk`（11,846,394 字节），已同签名覆盖安装到华为 Mate 20 Pro（`versionCode=10`）并真机复验。

### 步骤 29 发布「映语」0.3.2：人物卡独立壁纸/头像/声音、最新回复 💡 自动发送、朗读声波跳动与主界面动效

2026-11-24 按用户反馈完成 **映语 0.3.2**（`versionCode=9`，保持 `com.rolechat.app` 包名、同调试签名与 `schemaVersion=2`）：
- **每张人物卡独立对话壁纸与独立声音**：`CharacterCard` 新增 `wallpaperFile` 与 `avatarFile`；聊天页「更换本角色壁纸」仅更新当前人物卡及该角色对话，互不影响；删除或更换图片前做全库引用计数保护；确认并测试多人物卡各自独立保存音色配置。
- **角色头像默认取壁纸并支持自定义**：新增 `CharacterAvatar`（`ContentScale.Crop` + `Alignment.TopCenter` 自动聚焦人物面部），首页列表、聊天顶栏与编辑页统一展示；人物卡编辑页支持「自定义头像」与「恢复默认」。
- **💡 回复提示仅显示于最新回复 + 点击直接发送**：历史角色气泡不再显示 💡；点击展开的建议句直接自动发送，无需再点发送键。
- **朗读声波跳动与主界面按钮回弹**：`VoiceState` 追踪 `speaking` 与 `activeTag`（绑定 `message.id`），朗读中该消息的 `SoundWaveIcon` 实时交错跳动并显示「朗读中」；首页「接口设置」「创建人物卡」「开始聊天」「编辑」「删除」全部接入 `Modifier.bounceClick`。
- 涉及：`domain/Models.kt`、`voice/VoiceEngine.kt`、`voice/SystemVoiceEngine.kt`、`voice/SpeechController.kt`、`ui/AppIcons.kt`、`MainViewModel.kt`、`MainActivity.kt`、`app/build.gradle.kts`、`ConversationOpsTest.kt`。
- 验证结果：Gradle `testDebugUnitTest assembleDebug` 通过（9 个测试类、48 项 JUnit 全部通过，0 失败/0 错误/0 跳过），`tools/verify-project.mjs` 32/32 通过，`apksigner`（同证书 v2）与 `zipalign -c -P 16 -v 4` 通过；APK 输出为 `YingYu-v0.3.2-debug.apk`（11,813,626 字节），已同签名覆盖安装到华为 Mate 20 Pro 并真机复验。

### 步骤 28 发布「映语」0.3.1：气泡右侧 💡 AI 回复提示、朗读过滤旁白与按钮回弹动效

2026-11-24 按用户要求与参考图完成 **映语 0.3.1**（`versionCode=8`，保持 `com.rolechat.app` 包名、同调试签名与 `schemaVersion=2`）：
- **AI 回复提示（💡）**：每条角色回复气泡右下侧新增圆形玻璃 💡 按钮，点击按当前对话上下文调用模型生成 3 条简短回复建议，在气泡下方带平滑动画展开深色磨砂卡片，点击任意建议即填入输入框；再次点击 💡 可收起。
- **朗读过滤括号旁白**：新增 `SpeechTextFilter.spokenOnly` 并在 `SpeechController.speak` 统一过滤 `（...）`/`(...)`/`【...】`/`*...*` 动作与旁白，仅朗读人物台词（纯旁白句自动跳过不发请求）；同时聊天消息内通过 `AnnotatedString` 柔化括号旁白透明度，突出台词。
- **按钮回弹与面板动效**：新增 `ui/Motion.kt`（`Modifier.bounceClick` 弹簧缩放回弹），覆盖聊天页全部悬浮玻璃按钮、💡 按钮、建议卡片、发送按钮与首页主按钮，并为建议展开和底部外观面板增加 `AnimatedVisibility` 动效。
- 涉及：`voice/SpeechTextFilter.kt`（新增）、`voice/SpeechTextFilterTest.kt`（新增）、`ui/Motion.kt`（新增）、`ui/AppIcons.kt`、`voice/SpeechController.kt`、`MainViewModel.kt`、`MainActivity.kt`、`app/build.gradle.kts`。
- 验证结果：Gradle `testDebugUnitTest assembleDebug` 通过（9 个测试类、47 项 JUnit 全部通过，0 失败/0 错误/0 跳过），`tools/verify-project.mjs` 32/32 通过，`apksigner`（同证书 v2）与 `zipalign -c -P 16 -v 4` 通过；APK 输出为 `YingYu-v0.3.1-debug.apk`（11,797,242 字节），已同签名覆盖安装到华为 Mate 20 Pro 并真机复验。

### 步骤 27 发布「映语」0.3.0：默认自动朗读、静音图标、人物卡删除、消息回溯/删除与沉浸式聊天 UI

2026-11-24 按用户新需求与参考界面完成 **映语 0.3.0**（`versionCode=7`，保持 `com.rolechat.app` 包名、同调试签名与 `schemaVersion=2`）：
- **语音默认自动朗读 + 纯图标静音切换**：`TtsApiConfig.autoSpeak` 默认值调整为 `true`；移除底部「停止朗读」文字栏，改为聊天顶栏右上角纯矢量音量/静音图标按钮，一键关闭声音（立即停止播放并静音）或恢复自动朗读。
- **删除人物卡**：首页每张人物卡新增「删除」入口与确认弹窗，原子移除人物卡及对应历史对话。
- **消息回溯/删除与回溯人物**：新增 `Conversation.deleteMessage` / `rewindTo` / `resetToGreeting`；长按任意消息气泡弹出深色磨砂菜单（「从此句继续对话（回溯）」「复制」「朗读此句」「删除此句对话」）；聊天顶栏 `☰` 菜单新增「回溯人物（重回开场白）」及二次确认。
- **沉浸式聊天 UI 重构**：参考用户上传界面，将顶栏改为单行极简悬浮胶囊释放上半屏立绘空间，增加顶部「简介」玻璃浮卡、角色气泡上方声波朗读小胶囊，以及底部单胶囊 `发消息…` 输入栏（状态与重试条按需浮现）。
- 涉及：`ui/AppIcons.kt`（新增）、`domain/Models.kt`、`appearance/GlassBackdrop.kt`、`MainViewModel.kt`、`MainActivity.kt`、`app/build.gradle.kts`、`ConversationOpsTest.kt`（新增）、`AppDataCodecTest.kt`。
- 验证结果：Gradle `testDebugUnitTest assembleDebug` 通过（8 个测试类、43 项 JUnit 全部通过，0 失败/0 错误/0 跳过），`tools/verify-project.mjs` 32/32 通过，`apksigner`（同证书 v2）与 `zipalign -c -P 16 -v 4` 通过；APK 输出为 `YingYu-v0.3.0-debug.apk`（11,780,858 字节），已同签名覆盖安装到华为 Mate 20 Pro 并真机截屏复验首页与新沉浸式聊天页。

### 步骤 26 发布「映语」0.2.4：正式定名、自适应图标与内部界面美术优化

2026-11-24 用户选定星光对话气泡图标并确认正式定名为 **映语**，同时要求优化内部界面美术（`versionCode=6`，保持 `com.rolechat.app` 包名、同调试签名与 `schemaVersion=2`）：
- **品牌名与桌面图标**：更新 `AndroidManifest.xml` 为 `android:label="映语"` 及 `@mipmap/ic_launcher` / `@mipmap/ic_launcher_round`；生成 `mdpi`–`xxxhdpi` 常规/圆形 PNG 图标、Android 8+ 自适应图标（安全区缩放防裁切 + 匹配紫蓝渐变底）及首页品牌徽标 `ic_brand_logo.png`。
- **内部界面美术升级**：围绕图标的柔紫—晴蓝玻璃色系更新 Material 3 配色与环境渐变画布；首页顶栏展示品牌图标与「映语」标题，人物卡升级为珠光卡片（首字渐变头像、语音标签、引言区与圆角按钮）；人物编辑页与接口设置页按模块归入圆角珠光分卡片；聊天玻璃组件采用淡紫—晴蓝微虹彩高光描边，输入框与发送按钮改为圆角胶囊样式。
- 涉及：`AndroidManifest.xml`、`res/mipmap-*`、`res/drawable*`、`MainActivity.kt`、`GlassBackdrop.kt`、`app/build.gradle.kts`。
- 验证结果：Gradle `testDebugUnitTest assembleDebug` 通过（39 项 JUnit 全部通过），`tools/verify-project.mjs` 32/32 通过，`apksigner`（同证书 v2）与 `zipalign -c -P 16 -v 4` 通过；APK 输出为 `YingYu-v0.2.4-debug.apk`（11,731,706 字节），已同签名覆盖安装到华为 Mate 20 Pro（确认华为包安装器弹窗后安装成功）并完成真机首页视觉复验。

### 步骤 25 确认“映语”名称并生成三款图标候选

2026-11-24 用户接受名称“映语”，要求生成几个图标。使用内置 image_gen 各生成一张独立 PNG：叠映气泡、星光对话、声纹玻璃，均为 1254×1254。原图、完整最终提示词、尺寸与 SHA-256 保存在同级 Yingyu-Icon-Concepts；目视检查主题与构图，三张均无嵌入文字。

新增 BRAND_DESIGN，同步 AGENTS、HANDOFF、需求、架构、决策、调试与下一步。用户尚未选定最终图标；未替换 APK 名称或图标、未重新构建和安装。未修改其他 AI 已推进的 0.2.3 功能与联调记录。本轮无应用行为改动，不重复执行 Android 测试。

### 步骤 24 发布 0.2.3：高通透玻璃实时预览、自定义字体颜色、软件内调音与自动朗读

2026-11-24 响应用户反馈（玻璃白雾偏厚、希望自定义字体颜色、希望软件内调角色音色并增加自动朗读开关），完成 **0.2.3**（`versionCode=5`，保持原调试签名与 `schemaVersion=2` 向后兼容）：
- **高通透玻璃与底部实时预览**：移除 `GlassBackdrop` 写死的 13% 重白雾，改为极薄联动高光；默认 `glassOpacity` 调至 `0.06f`（94% 通透），滑块支持 `52%–100%` 与模糊 `0–32`；「外观」改为底部实时预览面板，上半屏壁纸与气泡实时可见。
- **自定义字体颜色**：`ChatAppearance` 新增 `textColorHex` 及亮度计算工具，提供 8 个快捷色点（纯黑/墨蓝/纯白/暖白/柔粉/浅青/深紫/深棕）与 `#RRGGBB` 自定义输入，自动联动文字阴影与状态栏/导航栏图标色。
- **软件内角色音色调节**：`VoiceConfig` 扩展 `cloudPitch`、`emotion`、`timbre`、`intensity`；`MiniMaxClient` 新增 8 个常用中文预设音色、`/v1/get_voice` 拉取账号音色，并在 `t2a_v2` 请求中透传音调、情绪与 `voice_modify`；抽取 `VoiceTuningSection` 同时接入人物卡编辑页与聊天页顶部「声音」弹窗（支持即时应用到当前对话并可选同步人物卡）。
- **自动朗读开关**：`TtsApiConfig` 新增 `autoSpeak`（默认 `false`），聊天页「声音」弹窗提供开关，保存设置不丢失该状态；开启后在 `MainViewModel.send` 保存新回复后自动朗读。
- 涉及：`Models.kt`、`GlassBackdrop.kt`、`MiniMaxClient.kt`、`MainViewModel.kt`、`MainActivity.kt`、`app/build.gradle.kts`、`AppDataCodecTest.kt`、`MiniMaxAudioTest.kt`。
- 验证结果：Gradle `testDebugUnitTest assembleDebug` 通过（7 个测试类、39 项 JUnit 全部通过，0 失败/0 错误/0 跳过），`tools/verify-project.mjs` 32/32 通过，`apksigner`（同证书 v2）与 `zipalign -c -P 16 -v 4` 通过。0.2.3 APK 已同签名覆盖安装到华为 Mate 20 Pro，真机实测用户自定义图片壁纸、底部实时预览面板、纯黑字体切换及聊天页「声音与自动朗读」弹窗均正常显示。

### 步骤 23 真机完成 DeepSeek 联调并发布 0.2.2 视觉与语音诊断修复

2026-11-24 在华为 Mate 20 Pro（LYA-AL00 / Android 10）完成 0.2.1 真机启动、Mobile MCP 元素读取/点击/输入/截图与真实 DeepSeek 接口验证：
- 现象/验证：用户在手机填入加密 Key 后，`GET /models` 在 `https://api.deepseek.com` 返回 2 个可用模型（`deepseek-flash`、`deepseek-v4-pro`）；选用 `deepseek-flash` 保存并创建人物卡 `Lin`（含设定与开场白）发起单轮对话，成功收到中文回复「你好，欢迎光临。」，未再复现 HTTP 400。强杀进程后冷启动，人物卡、聊天历史与加密密钥均正常恢复。
- 真机发现 4 项问题并修复至 **0.2.2**（versionCode=4，保持原调试签名、数据格式仍为 v2）：
  1. 聊天页外层受 `Scaffold` 系统栏 padding 限制，上下出现白色系统条 → 聊天路由释放 `contentWindowInsets`，让 `WallpaperBackground` 全屏铺底，内部栏位改用 `statusBarsPadding()`、`navigationBarsPadding()`、`imePadding()`，并由 `WindowCompat` 随 `darkText` 同步状态栏/导航栏图标明暗。
  2. 深色玻璃上的禁用态「发送」按钮与 `TextButton` 沿用默认深色文字，对比度过低 → 显式指定基于 `foreground` 的 `disabledContentColor` 与自适应 `BorderStroke`。
  3. 手机窄屏下 560dp 气泡上限近似满宽，左右阵营区分不够明显 → 通过 `BoxWithConstraints` 将气泡最大宽度限定为视口 84%（上限 560dp）。
  4. 点击「朗读」请求 `https://api.minimax.cn/v1/t2a_v2` 返回 MiniMax 业务码 `1004` 时，原实现丢弃 `base_resp.status_msg` 且底部状态栏 2 行截断 → `MiniMaxClient.decodeAudio` 接入 `ApiFailures.clean` 输出脱敏 `status_msg`（最多 200 字），聊天状态栏改为 4 行省略展示，设置页补充国内站（`https://api.minimaxi.com/v1/t2a_v2`）与国际站（`https://api.minimax.io/v1/t2a_v2`）快捷地址按钮。
- 涉及：`MainActivity.kt`、`MiniMaxClient.kt`、`app/build.gradle.kts`、`MiniMaxAudioTest.kt`。
- 验证结果：`testDebugUnitTest assembleDebug` 通过（7 个测试类、37 项 JUnit 全部通过，0 失败/0 错误/0 跳过），`tools/verify-project.mjs` 32/32 通过，`apksigner`（同证书 v2）与 `zipalign -c -P 16 -v 4` 通过。0.2.2 APK 已同签名覆盖安装到华为真机，复验全屏沉浸壁纸、84% 气泡宽度、禁用按钮对比度正常，且点击「朗读」已完整读出服务端 `1004` 说明：`login fail: Please carry the API secret key in the 'Authorization' field of the request header`。
- 未完成项：MiniMax 语音真实发声（待用户核对语音 Key 类型/地域端点后实测）、自定义照片壁纸与浅色壁纸实测、系统 TTS 与离线音色实测、人物卡修改快照与历史切换完整回归。

### 步骤 22 安装手机控制工具并连接真机

2026-11-24 用户接受 Mobile MCP 主控、scrcpy 辅助方案。固定安装 Mobile MCP 1.0.19（本地 npm 包），下载校验 scrcpy 5.0.1，配备 ADB 37.0.3，保存到 Documents/Codex/tools/mobile-control。新增本地 launcher 和 stdio 探测脚本，Codex CLI 注册 mobile-mcp 成功，独立初始化、35 个工具发现和真机枚举通过。

用户初期开启的是 HDB；确认真正 USB 调试开启后，华为 Mate 20 Pro / LYA-AL00 / Android 10 的 ADB 状态为 device，驱动 ADB Interface 为 OK。用户说明该机未安装本应用，已定向安装 RoleChat-v0.2.1-debug.apk，ADB 返回 Success。尚未启动应用、投屏、读取界面或调用真实 API；本轮未改应用代码或重新构建。

配置事先作当前 Windows 用户 DPAPI 加密备份。核对发现除新服务器外 node_repl.args 与备份不同，原因待查，未整体回滚；详见 HANDOFF。用户因额度要求交接，已保存完整工程状态与下一步，维护六份工程文档。此前源码 ZIP 未重打包，以工程目录及 HANDOFF 为最新记录。

### 步骤 21 明确自动调试选型与 Root 条件

2026-11-24 用户询问后两项是否需要 root 和哪项更合适。核对 Mobile MCP 真机前置条件与 uiautomator2 的 ADB/UiAutomator 工作方式：普通 UI 操作和调试无需 root。建议以 Mobile MCP 作为后续 AI 自动操作主工具，scrcpy 辅助投屏，现有 ADB 先验证连接；uiautomator2 留给固定回归。同步方案与相关文档，没有安装、接入、连接或代码变更。

## 2026-11-24 电脑连接手机的工具调研

### 步骤 20 核对手机调试能力与 GitHub 项目

用户询问连接手机后能否由电脑操作，并要求搜索相关 GitHub 项目。核对已有 ADB 路径，检查 PATH 中没有 scrcpy；查阅 Genymobile/scrcpy、mobile-next/mobile-mcp、openatx/uiautomator2 的官方项目和 Android 真机连接说明。

建议先 ADB + scrcpy 建立 USB 真机调试，Mobile MCP 与 uiautomator2 作为后续自动化选项。新增 DEVICE_DEBUGGING，记录能力、当前未连接状态、授权步骤和 DeepSeek 400 的后续验证路径；同步需求、架构、决策、调试和下一步。

本轮是资料与方案更新，没有安装工具、接入 MCP、修改应用代码或重新编译 APK；此前 0.2.1 的 36 项测试结果不代表已验证手机控制。

## 2026-11-24 DeepSeek 诊断与兼容更新 0.2.1

用户反馈官方地址与正确密钥仍有接口错误，补充手机截图确认 HTTP 400。旧版未展示服务端具体说明，不能由此确认唯一根因；当前未获得用户 Key，也没有真实 API 成功验证。

### 步骤 16 恢复具体错误证据

ChatClient 有界读取错误体，只提取服务端 code/type/message 和可用请求编号，区分格式、认证、余额、限流、服务端与网络故障。新增 ApiFailure，全局弹窗保留到关闭并支持复制；已知 Key 与常见 token 先脱敏，不把整段 HTML 或请求头写入提示和日志。

涉及：ApiFailure、ChatClient、MainViewModel、MainActivity。

### 步骤 17 调整请求兼容并增加模型发现

开场白继续显示与保存，但构造请求时放到 system 的已说过上下文，实际轮次从 user 开始。仅在 api.deepseek.com 上显式发送 thinking.type=disabled。新增官方预设、GET /models 测试和模型选择，测试不发聊天正文、不自动保存配置，不自动重试生成。

这些是兼容性改进，不把“开场白顺序”或“默认思考”冒充已经由服务端说明证实的 400 原因。

### 步骤 18 回归与覆盖安装包

增加 17 项测试，共 7 个测试类、36 项全部通过。覆盖截图近似消息夹具、DeepSeek 路由/请求参数、错误提取与脱敏、402、模型列表 GET、格式错误和单次失败不自动重试；模拟响应来自本地应用拦截器，不访问真实 DeepSeek。

Android 构建成功，版本名 0.2.1、版本码 3；与 0.2.0 相同的测试签名，APK 签名和 ZIP 对齐检查通过。数据格式仍为 v2。旧 APK、源码与 19 项测试的历史报告保留；本轮报告独立保存。

### 步骤 19 保存排查与等待真实结果

新增 API_DIAGNOSTICS，同步需求、原理、决策、调试和安装方法。已提供新版 APK，下一步由用户在手机执行模型列表测试并重试原会话；若失败，取得新版脱敏错误说明后继续定位。不得把 36 项模拟/单元测试当成真实账号修复验证。

## 2026-11-24 交付首个可安装 APK

用户询问“怎么安装，apk 文件在哪里”。此前只有源码 ZIP；本轮补齐实际构建、验证、测试包与安装文档，软件版本保持 0.2.0。

### 步骤 12 准备便携构建工具

在工作区内下载并校验 JDK 17，准备 Android 命令行工具、SDK 35、Build Tools 35.0.0 与 Gradle 8.11.1。缓存、SDK 用户目录与调试密钥均留在工作区，不改系统全局环境。常见路径检查外还发现系统厂商附带的精简 Java 运行时，但它没有完整编译能力，所以仍使用便携 JDK。

### 步骤 13 修正 Wrapper 配置入口

首次下载超时，定位到最小 gradlew/gradlew.bat 未读取 JAVA_OPTS 和 GRADLE_OPTS。两个入口加入支持，分发包下载超时从 10 秒提高至 60 秒，原始 SHA-256 校验保留。一次构建从错误目录启动后改为显式 -p 指定工程目录。

涉及：gradlew、gradlew.bat、gradle-wrapper.properties。没有更改产品功能或数据格式。

### 步骤 14 实际编译与验证

命令 testDebugUnitTest assembleDebug 执行成功，Gradle 报告 BUILD SUCCESSFUL，41 项任务执行。5 个测试类共 19 项测试，失败、错误与跳过均为 0。Kotlin 与 Android 资源编译通过。

生成调试 APK 后，apksigner 验证 v2 签名通过；zipalign -c -P 16 -v 4 通过。元数据确认包名 com.rolechat.app、versionName 0.2.0、versionCode 2、最低 SDK 26、目标 SDK 35。原始报告保存在 docs/checks/android-build。

构建工具提示 libandroidx.graphics.path.so 无法剥离调试符号，因此保持原库打包；这是构建提示，未导致打包失败。没有因此声称已完成原生库的全部设备兼容验证。

### 步骤 15 保存可安装包与安装说明

将构建产物复制到交付目录 RoleChat-v0.2.0-debug.apk，记录文件大小和 SHA-256。新增 INSTALL，更新 README、需求、架构、决策、调试、验证和后续步骤。源代码新压缩包排除编译缓存、build 目录和签名密钥；既有源码 ZIP 保留。

仍未完成：真机运行、玻璃视觉与性能、系统语音与播放、Android 文件迁移故障恢复、真实模型与语音 API 联调。这些不能由编译和 JVM 单元测试代替。

## 2026-11-24 玻璃聊天界面 0.2.0

用户追加需求：聊天框使用高斯模糊或类似液态玻璃的通透材质，让聊天壁纸可见。

### 步骤 8 增加壁纸与外观存储

新增全局 ChatAppearance，提供本地照片选择、默认背景、通透程度、模糊强度与文字深浅。选中的图片在后台缩小、纠正方向并复制到私有目录，避免原 URI 失效。先保存新图片与配置，再清理旧图片；壁纸不加入模型请求。

涉及：Models、WallpaperStore、MainViewModel、MainActivity、示例 JSON。

### 步骤 9 实现局部高斯背景与玻璃层

保持清晰的原壁纸，生成共享的低分辨率高斯缓存。按各组件的根坐标与共同居中裁剪取样，给消息气泡、聊天栏和输入栏绘制圆角玻璃、薄色与高光边。文字最后绘制，不参加模糊；滚动时只更新取样位置。

涉及：GaussianBlur、GlassBackdrop、MainActivity。未加入动态折射或视频背景。

### 步骤 10 保护旧版记录

数据格式升至 schemaVersion=2。将缺失版本号的旧 JSON 识别为 v1，保留原始字节副本后再写新格式；保留旧主文件名以找到既有记录。提取 AppDataCodec 用于版本与默认字段检查。

涉及：AppDataCodec、LocalStore、Models、DATA_MODEL。迁移的 Android 文件故障恢复尚待实测。

### 步骤 11 保存原理与验证状态

新增 GLASS_DESIGN，更新需求、架构、决策、调试和后续步骤。新增 8 个 JUnit 方法，检查高斯基本性质和旧数据格式兼容，加上原有 11 个共 19 个；这些 JUnit 仍未执行。Kotlin 语法和结构检查结果见 VALIDATION。

旧版本 ZIP 保留，本版单独输出 0.2.0 源码 ZIP。当前没有 JDK/Android SDK，因此不把视觉效果、帧率、迁移或 Android 编译记为通过。

## 2026-11-24 开发基线 0.1.0

### 步骤 1 建立需求与接口边界

记录用户提出的 Android 聊天、API 配置、人物卡五个字段、语音调研与全程保存方案要求。定义人物、会话、消息、声音和配置的数据结构，采用可替换的聊天与语音边界。

涉及：domain/Models.kt、REQUIREMENTS、DATA_MODEL、AGENTS.md。

### 步骤 2 编写人物卡与聊天路径

加入 Compose 人物列表、编辑、设置和聊天界面；角色快照、开场白、历史裁剪、保存和历史切换；按基础 URL 调用兼容 Chat Completions 接口。失败保留用户消息，可重试、撤回或停止本机请求。

涉及：MainActivity、MainViewModel、PromptBuilder、ChatClient。

### 步骤 3 保存与密钥保护

加入 AtomicFile 版本快照与损坏保护；读写在 IO 线程，提交先落盘再更新状态；保存阶段用 NonCancellable 避免取消后磁盘和内存不同步。Key 独立 AES-GCM 加密，由 Android Keystore 提供加密材料。

涉及：LocalStore、KeyVault、MainViewModel。

### 步骤 4 调研并编写系统语音路径

核对 sherpa-onnx Android 部署资料、VITS 与 Kokoro 中英文模型和系统引擎 APK。编写系统 TTS 音色枚举、离线筛选、语速音调与朗读；本地模型尚未内置。

涉及：VoiceEngine、SystemVoiceEngine、VOICE_RESEARCH。

### 步骤 5 根据用户补充改为云端语音优先

用户选择“优先自然、有角色感的云端语音”。新人物默认 CLOUD，加入 MiniMax 语音配置、独立 Key、人物音色 ID、试听与点击朗读、MP3 播放、取消和缓存清理。系统语音保留为用户可选提供商。未自动改用其他提供商。

涉及：MiniMaxClient、SpeechController、Models、KeyVault、MainActivity、MainViewModel。

### 步骤 6 增加检查与可重复构建入口

加入官方 Gradle Wrapper，固定版本并核对官方 SHA-256。加入 11 个 JUnit 测试方法，覆盖历史裁剪、角色快照、地址规则、音频解码与供应商错误；加入结构检查脚本和 GitHub Actions 模板。Kotlin 文件通过语法解析，XML 通过解析，结果在 VALIDATION 中分开记录。

首次语法检查发现两处异常处理写法无法被解析器识别，统一为完整 catch 块后检查通过。播放路径同时调整为先记录播放器再设置数据源，确保设置失败也能释放资源。

未执行：Android 编译、JUnit、真实聊天 API、真实语音 API、模拟器或真机运行。原因：当前环境没有 JDK/Android SDK，也未配置服务账号和 Key。源码中的实际 Android 行为仍以这些后续验证为准。

### 步骤 7 保存交付与后续约定

工程内保存需求、程序原理、数据结构、语音来源、方案决策、调试方法、验证记录和下一步。交付工程目录与源码 ZIP。后续每次增改按 AGENTS.md 同步维护这些文档。
