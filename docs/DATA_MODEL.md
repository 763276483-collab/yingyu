# 数据模型与格式

主数据仍位于应用私有 `noBackupFilesDir/rolechat-v1.json`，保留文件名以找到旧记录；当前逻辑格式为 `schemaVersion = 2`，软件版本为 0.2.1。新版显式编码全部默认字段和版本号；未知字段可忽略，未知版本不覆盖。

AppDataCodec 将缺少版本号的旧文件视为 v1（旧版会省略默认版本字段）。读取显式 v1 或缺省版本时，LocalStore 先把原始字节保存到 `rolechat-v1.pre-v2.json`，再加入默认 appearance 并原子写入 v2。已有备份不覆盖；解析、备份或迁移失败均停止加载。该迁移仍待 Android 文件读写实测。

## 人物卡

| 字段 | 含义 | 是否进入聊天请求 |
| --- | --- | --- |
| id | UUID，不随改名变化 | 否 |
| name | 角色名字 | system |
| persona | 身份、背景、性格、口吻等设定 | system |
| greeting | 开场白 | 新对话首条 assistant |
| description | 给用户看的简介 | 否 |
| voice | 角色声音配置 | 只用于语音 |
| updatedAt | 毫秒时间戳，编辑后更新 | 否 |

声音配置：provider 目前可选 CLOUD 或 SYSTEM；LOCAL_ONNX 保留。voiceId 在云端为 MiniMax 音色 ID，在系统模式为 Android Voice.name。language 默认 zh-CN，speed 默认 1。pitch 是系统 TTS 的音调比例，云端暂固定中性音调；offlineOnly 只在系统模式生效。enginePackage 与 modelId 为后续引擎管理预留，当前不会据此加载模型。

角色声音与“角色设定中写着温柔女声”是两件事：只有 voiceId 等实际参数才选择合成音色。当前没有把角色设定自动转成声音，也没有声音克隆。

## 会话与消息

Conversation 包含 id、character 人物快照、createdAt、messages。可以为同一人物创建多段对话。旧对话保留原人物快照；主列表编辑人物卡不会改旧记录。

Message 包含 id、role、content、createdAt。消息列表中的顺序就是对话顺序。最后一条为 USER，且应用没有生成任务时，表示等待回复，可以重试或撤回。无需另存一个“正在生成”的持久标记，从而在进程死亡后也能恢复这个状态。

0.2.1 的开场白兼容调整只发生在请求构造阶段，不改 Message 的存储角色或正文。ApiFailure、可用模型列表和模型列表对应地址只属于 AppState 的内存状态，不持久化，因此本次不新增数据迁移。

## 配置与密钥

ApiConfig 存聊天 baseUrl、model、contextTurns。TtsApiConfig 存语音完整 endpoint 与 model。KeyVault 不属于 AppData：`chat-api-key.enc` 和 `tts-api-key.enc` 分别保存 AES-GCM 的 IV + 密文，密钥材料留在 Android Keystore。

示例 `examples/app-data.example.json` 不含真实 Key。此版本尚无导入按钮，不要把它复制成手机正式数据；它用于理解结构和后续迁移检查。

## 聊天外观

AppData.appearance 为全局外观，独立于人物快照，修改后所有聊天一起更新。

| 字段 | 默认值与含义 |
| --- | --- |
| wallpaperFile | 空字符串使用默认壁纸；否则只保存应用私有目录内的生成文件名，不保存原照片 URI |
| glassOpacity | 0.18，透色层浓度；范围 0.05–0.48，界面以反向“通透程度”显示 |
| blurRadiusDp | 12，背景高斯标准差的视觉强度参数；0 关闭模糊，最大 32 |
| darkText | false，默认浅色文字；浅壁纸可改为深色文字 |

壁纸保存在 noBackupFilesDir/wallpapers，不进入模型请求，也不上传至语音服务。通过系统文件选择器读取后，复制、缩小和纠正方向，转换成最长边不超过 1600 像素的 JPEG。替换时先保存新图片与配置，再清理上一张壁纸；恢复默认保留其他玻璃参数。

## 迁移约定

1. 修改持久化含义前，先更新 schemaVersion 与迁移说明，不能只改文件名。
2. 迁移前保留旧文件；先完整读入和校验，再写新格式，成功后再切换。
3. 用户导出功能默认不导出密钥。角色卡导出需独立定义 schema，不能直接假装兼容其他聊天软件的 PNG 人物卡格式。
4. 云端语音供应商增加后，voiceId 必须和供应商身份一起保存，避免相同字符串被错误发送到另一供应商。
5. 语音模型版本当前是全局配置，旧对话只冻结人物音色，不冻结全局语音模型或聊天模型；需要精确复现时，还要保存模型、提示词版本和推理参数快照。
