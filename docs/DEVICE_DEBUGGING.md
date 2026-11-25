# 电脑连接手机的工具选择与调试步骤

调研日期：2026-11-24。用户希望连接手机后由电脑辅助调配，并要求到 GitHub 查找相关项目。目标仍是验证“角色聊”、读取具体接口错误并继续修复 DeepSeek 400。

## GitHub 候选

| 项目 | 官方说明中的能力 | 本工程的用途 |
| --- | --- | --- |
| [Genymobile/scrcpy](https://github.com/Genymobile/scrcpy) | Windows 等平台投屏，USB 或网络连接，键盘鼠标控制，不需要 root | 先用于观察界面、操作设置与复现问题；配合已有 ADB |
| [mobile-next/mobile-mcp](https://github.com/mobile-next/mobile-mcp) | 向 AI 代理提供界面结构、截图、点击滑动、应用启动与安装等工具；支持真实设备 | 需要持续自动操作时的候选，必须另外安装并接入，不能把找到仓库当成已获得控制能力 |
| [openatx/uiautomator2](https://github.com/openatx/uiautomator2) | Python 调用 Android UiAutomator 能力，按元素定位和执行操作 | 后续编写重复执行的回归脚本，例如打开设置、测试连接、选择模型 |

官方补充资料：[scrcpy Windows 说明](https://github.com/Genymobile/scrcpy/blob/master/doc/windows.md)、[Mobile MCP Android 真机说明](https://github.com/mobile-next/mobile-mcp/wiki/Getting-Started-with-Android-Physical-Device)。

## 本轮选择与实际状态

用户已接受 Mobile MCP 主控、scrcpy 辅助方案。Mobile MCP 1.0.19、scrcpy 5.0.1、ADB 37.0.3 已安装在 `<tools-dir>/mobile-control`。Codex CLI 已登记 mobile-mcp，独立 stdio 初始化、工具发现和真实设备枚举通过；原生工具组在当前会话尚未热加载，必要时重启 Codex 后检查。

实际设备为华为 Mate 20 Pro，LYA-AL00，Android 10/API 29，ADB 已授权为 device，MCP 显示 online。已安装 0.2.1，ADB 返回 Success。尚未打开应用、投屏或操作界面；未验证真实 DeepSeek/TTS。交接入口和配置核对待办见工程根目录 HANDOFF.md。

## Root 与权限要求

Mobile MCP 与 uiautomator2 的常规 Android 界面操作、截图、输入和测试不要求手机 root。前者的官方真机条件为 ADB、已开启并授权的 USB 调试，后者通过 ADB 和 UiAutomator 的设备端服务工作；均需要完成实际连接和权限确认。[Mobile MCP 真机条件](https://github.com/mobile-next/mobile-mcp/wiki/Getting-Started-with-Android-Physical-Device)、[uiautomator2 工作方式和快速开始](https://github.com/openatx/uiautomator2)。

USB 调试授权不等于 root。部分设备的输入控制还受厂商设置限制，要连接后按实际提示处理。本工程安装 APK、点击界面、读取本应用错误和测试聊天不需要为此 root 手机。上述结论仅针对普通调试用途，不代表可以任意读取系统或其他应用的私有数据。

用户已授权上述工具接入和手机调试，本轮完成安装、枚举与 APK 安装，没有修改 Android 应用代码或重新打包 APK。新 ADB 固定路径为工具目录 `android-sdk/platform-tools/adb.exe`；仍需验证截图和输入才可声称 UI 控制已实测。

scrcpy 下载包官方 SHA-256：`b12a2c4ee8be317422451fc7dcf8ee20a71b5ea7ef9ad73ddd825a25316227a5`，已核对匹配，版本命令返回 5.0.1。Mobile MCP 固定本地 npm 包由 Node 24.22.0 启动，launcher 设置进程专用路径并关闭支持的遥测，不在运行时 npx 下载。

## 首次 USB 连接

1. 使用支持数据传输的数据线连接这台电脑，并解锁手机。
2. 在手机“开发者选项”开启 USB 调试。不同品牌入口不同，找不到时根据手机型号查看官方步骤。
3. 手机弹出此电脑的调试授权时，由用户在手机上确认。Windows 若仍无法识别，按设备厂商驱动说明处理。
4. 执行 adb devices -l。只有目标设备显示 device 才进入后续操作；unauthorized 表示尚未授权，offline 表示连接异常。多设备时始终指定序列号。
5. 再验证画面获取和输入权限。部分设备除 USB 调试外还需要单独允许输入控制；按实际系统提示处理，不预设全部机型行为相同。

连接依据：[Android 真机开发步骤](https://developer.android.com/studio/run/device)、[Android ADB 文档](https://developer.android.com/tools/adb)。

## 连接后的调试范围

先检查设备型号、系统版本和应用版本，再同签名覆盖安装最新 APK、启动角色聊、复现模型列表与聊天请求，读取应用相关错误。必要时通过投屏或界面元素操作，观察玻璃取样、键盘、滚动和语音播放。

API Key 继续由用户在手机应用中填写，不从手机导出密钥文件。错误反馈优先使用 0.2.1 的已脱敏详情；不要求抓取全部手机数据。没有确认设备前，不执行安装或输入操作。

## 后续验证记录

已记录：ADB 识别、USB 调试授权、MCP 真机枚举、0.2.1 首次安装成功。用户最初开启 HDB，真正开启 USB 调试后 ADB 才连通；Windows 的 ADB Interface 已为 OK，HDB Error 不影响当前连接。

尚待记录：应用启动、投屏与输入、GET /models、真实聊天和语音、玻璃视觉效果。用户要求切换 AI，后续从 HANDOFF 继续；DeepSeek 400 仍未确认根因与修复结果。配置核对发现 node_repl.args 与登记前加密备份不同，未整体回滚，接手者须私下核对。
