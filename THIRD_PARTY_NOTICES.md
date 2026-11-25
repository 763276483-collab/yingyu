# 第三方组件说明

工程包含从 Gradle 官方仓库下载的 Gradle 8.11.1 Wrapper JAR，保持原样，相关许可位于 JAR 内 META-INF/LICENSE。来源：https://github.com/gradle/gradle/tree/v8.11.1/gradle/wrapper 。启动脚本是本工程的最小入口，构建分发包由官方服务下载，并按官方 SHA-256 验证。

Android、Compose、Kotlin、Coroutines、Serialization、OkHttp 与 JUnit 通过构建配置引用，未将它们的发布包复制到本源码交付中。发布 APK 前应根据实际解析到的依赖生成并附带相应许可与声明。

本工程没有附带语音模型权重。以后打包本地语音模型时，分别核对推理代码、模型权重、词典、音素组件和音色素材的许可。
