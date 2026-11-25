# 0.3.8：前文收起、夜间玻璃与接入引导

日期：2026-11-24。用户提供 0.3.7 APK、交接文件和两段录屏，要求修复收起聊天、将首页改成深色玻璃效果，并详细介绍 DeepSeek/MiniMax API 配置。

## 录屏现象与修复边界

两段录屏分别长约 8.17 秒（720×1024）和 8.34 秒（720×1056）。切换按钮文字已改变，但旧消息仍可见，消息区域位置跳动。不是删除历史的需求：聚焦模式只改变展示窗口，完整会话仍然保存并参与原有上下文裁剪。

旧实现将简介、旧消息、切换按钮和最新消息放在同一个 LazyColumn，同时切换顶部/底部对齐，保留同一 LazyListState，并对气泡使用 animateItem。这样模式变化时，旧锚点、自动滚动与移除动画会交叉作用。此次按该状态耦合修复，并通过真实 UI 验证，不把它归因于 API 或数据损坏。

新实现：

1. 切换胶囊移到滚动消息区域外，保留稳定的 48dp 最小触区；不随历史移动，也不参加消息位移动画。
2. 用 `key(chat.id, showFullHistory)` 重建模式对应的列表状态和布局；切换时直接定位最新消息，不把旧索引和退出图层带入新模式。
3. 消息统一由 `visibleMessages` 渲染：展开为完整列表，收起为 latestTurnWindow；移除这一列表的 animateItem。聚焦仍底部对齐，长消息仍可滚动。
4. 打字机进度改存于会话级 Map，切换列表不从头打字。没有可隐藏历史时恢复聚焦；不改变消息持久化、API 历史或角色声音。

## 夜间主页

`ui/NightGlass.kt` 定义墨蓝/紫色 Material 3 暗色主题及静态夜景背景。首页、人物编辑和接口设置共用一张背景和低分辨率模糊缓存；`SectionCard` 使用同坐标取样的 GlassSurface，薄色叠层和低亮度单层描边，不叠加投影。夜间说明文字、按钮、输入框、对话框和系统栏统一适配；删除类文字使用暗色主题的 error 颜色。

`rememberGlassBackdrop` 新增可选默认背景生成器；聊天原有默认壁纸、个人壁纸、字体颜色与陀螺仪视差保持原有数据来源。首页采用静态柔光背景，不开启陀螺仪或逐帧重算模糊。`GlassSurface.borderAlpha` 默认 1 保持既有聊天边缘，新夜间卡片降低描边亮度。

这是基于共享背景高斯取样与高光的玻璃外观，不声称实现苹果系统的真实动态折射。

## 接口设置引导

按“文字聊天 / DeepSeek”“角色语音 / MiniMax”分区，解释 API 和 Key、各服务用途、基础地址与完整 URL 的区别、模型 ID 与人物名/音色的区别，以及 Key 留空保留和保存后生效。

- DeepSeek：平台官网创建 Key → 官方预设 → 粘贴 Key → GET /models 测试 → 选择真实返回的模型 → 保存 → 回聊天发送消息。默认建议从 deepseek-flash 开始，不承诺其他模型始终可用或固定价格。
- MiniMax：选择申请 Key 的国内/国际站 → 选择语音模型 → 保存 → 到人物卡/声音面板拉取音色和试听。提供 speech-2.8-hd / speech-2.8-turbo 快捷选项，解释自动朗读和实际调用计费。
- 新预设国内完整 URL 为 `https://api.minimax.cn/v1/t2a_v2`，国际为 `https://api.minimax.io/v1/t2a_v2`。只在用户点击按钮时填入，不迁移或覆盖现有保存地址与 Key。
- 官网按钮通过系统浏览器打开；网址也显示为可选择文字，浏览器不可用时提示复制。这里的点击不会自动获取密钥、开通服务或发起生成。

2026-11-24 核对的官方来源：

- [DeepSeek 首次调用 API](https://api-docs.deepseek.com/zh-cn/)、[DeepSeek 开放平台](https://platform.deepseek.com/)。
- [MiniMax 国内站前置准备](https://platform.minimax.cn/docs/guides/quickstart-preparation)、[国内同步语音合成](https://platform.minimax.cn/docs/api-reference/speech-t2a-http)、[国际同步语音合成](https://platform.minimax.io/docs/api-reference/speech-t2a-http)。
- [Compose 惰性列表与元素动画](https://developer.android.com/develop/ui/compose/lists#item-animations)。

## 验证与交付

版本 0.3.8 / versionCode 15，包名与原调试证书不变，schemaVersion 仍为 2。55 项既有 JUnit 和 Android 编译通过，APK 签名与 ZIP 对齐通过，记录位于 checks/android-build-0.3.8。

华为 Mate 20 Pro / Android 10 已覆盖安装。在现有 31 条消息的对话上完成 6 轮展开/收起：12 次标签匹配，所有收起状态都只剩最新两条消息，最新内容保持一致。新首页已目视检查。初次同时操作手机的混合批次已作废，没有据此宣布通过；用户确认不再操作后重新采样。

宽屏尺寸模拟和设置页检查的最终结果见该目录的 device-validation.md。实际平板/折叠屏、长回复生成过程中的切换和键盘/旋转组合仍应按真实硬件继续验收。本轮回归脚本不发送聊天或调用语音，未导出 Key、清数据或删除用户消息。
