package com.rolechat.app

import android.app.Application
import android.net.Uri
import com.rolechat.app.appearance.WallpaperStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rolechat.app.data.KeyVault
import com.rolechat.app.data.LocalStore
import com.rolechat.app.domain.*
import com.rolechat.app.network.ChatClient
import com.rolechat.app.network.CloudVoicePreset
import com.rolechat.app.network.ApiFailure
import com.rolechat.app.network.ApiException
import com.rolechat.app.network.MiniMaxClient
import com.rolechat.app.voice.SpeechController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppState(
    val data: AppData = AppData(),
    val loaded: Boolean = false,
    val fatal: Boolean = false,
    val busy: Boolean = false,
    val sending: Boolean = false,
    val hasKey: Boolean = false,
    val hasTtsKey: Boolean = false,
    val notice: String? = null,
    val apiFailure: ApiFailure? = null,
    val availableModels: List<String> = emptyList(),
    val modelsBaseUrl: String = "",
    val availableCloudVoices: List<CloudVoicePreset> = emptyList(),
    val suggestionsMessageId: String? = null,
    val suggestions: List<String> = emptyList(),
    val loadingSuggestions: Boolean = false,
    val animatingMessageId: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LocalStore(application)
    val wallpapers = WallpaperStore(application)
    private val vault = KeyVault(application)
    private val ttsVault = KeyVault(application, "tts")
    private val client = ChatClient()
    private val mutable = MutableStateFlow(AppState())
    val state = mutable.asStateFlow()
    val voice = SpeechController(application, viewModelScope) {
        state.value.data.ttsApi to withContext(Dispatchers.IO) { ttsVault.read() }
    }
    private var requestJob: Job? = null
    private var suggestionJob: Job? = null

    init { viewModelScope.launch {
        try {
            val data = store.load()
            var hasKey = false
            var hasTtsKey = false
            var warning: String? = null
            try {
                hasKey = withContext(Dispatchers.IO) { vault.read().isNotBlank() }
            } catch (e: Exception) {
                warning = "密钥读取失败，请在设置中重新保存 API Key。"
            }
            try { hasTtsKey = withContext(Dispatchers.IO) { ttsVault.read().isNotBlank() } }
            catch (e: Exception) { warning = "语音密钥读取失败，请重新保存云端语音 Key。" }
            mutable.value = AppState(data = data, loaded = true, hasKey = hasKey, hasTtsKey = hasTtsKey, notice = warning)
        } catch (e: Exception) {
            mutable.value = AppState(loaded = true, fatal = true,
                notice = "本地数据无法读取，已停止写入以保留原文件。请查看工程调试文档。")
        }
    } }

    private fun available(): Boolean = state.value.loaded && !state.value.fatal && !state.value.busy
    private suspend fun persist(data: AppData) = withContext(NonCancellable) {
        store.save(data)
        mutable.update { it.copy(data = data) }
    }
    private fun action(block: suspend () -> Unit) {
        if (!available()) return
        mutable.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                mutable.update { it.copy(apiFailure = e.failure) }
            } catch (e: Exception) {
                mutable.update { it.copy(notice = e.message ?: "操作失败，请稍后重试。") }
            } finally {
                mutable.update { it.copy(busy = false) }
            }
        }
    }
    fun clearNotice() { mutable.update { it.copy(notice = null) } }
    fun clearApiFailure() { mutable.update { it.copy(apiFailure = null) } }
    fun finishMessageAnimation(messageId: String? = null) {
        mutable.update {
            if (messageId == null || it.animatingMessageId == messageId) it.copy(animatingMessageId = null) else it
        }
    }

    fun testApi(config: ApiConfig, replacementKey: String) = action {
        mutable.update { it.copy(apiFailure = null, availableModels = emptyList(), modelsBaseUrl = "") }
        val key = if (replacementKey.isNotBlank()) replacementKey else withContext(Dispatchers.IO) { vault.read() }
        val models = client.listModels(config, key)
        mutable.update { it.copy(availableModels = models, modelsBaseUrl = config.baseUrl.trim(),
            notice = "连接成功，读取到 ${models.size} 个模型；请选好模型后保存设置。") }
    }

    fun saveAppearance(config: ChatAppearance, onSaved: () -> Unit) = action {
        require(config.glassOpacity.isFinite() && config.blurRadiusDp.isFinite()) { "外观参数无效。" }
        val rawHex = config.textColorHex.trim()
        val hex = if (rawHex.isEmpty()) "" else ChatAppearance.normalizeHex(rawHex).also {
            require(it.isNotEmpty()) { "字体颜色请填写 6 位十六进制色值，例如 #111111 或 #FFFFFF。" }
        }
        val candidate = config.copy(
            wallpaperFile = state.value.data.appearance.wallpaperFile,
            glassOpacity = config.glassOpacity.coerceIn(0f, 0.48f),
            blurRadiusDp = config.blurRadiusDp.coerceIn(0f, 32f),
            textColorHex = hex,
        )
        val appearance = candidate.copy(darkText = candidate.isDarkTextColor())
        persist(state.value.data.copy(appearance = appearance))
        onSaved()
    }

    fun fetchCloudVoices() = action {
        val key = withContext(Dispatchers.IO) { ttsVault.read() }
        val fetched = MiniMaxClient().listVoices(state.value.data.ttsApi, key)
        mutable.update {
            it.copy(
                availableCloudVoices = fetched,
                notice = if (fetched.isEmpty()) "未读取到账号音色，可继续使用内置音色或手动填写 ID。"
                else "已读取 ${fetched.size} 个云端可用音色。",
            )
        }
    }

    fun setAutoSpeak(enabled: Boolean) = action {
        if (!enabled) voice.stop()
        persist(state.value.data.copy(ttsApi = state.value.data.ttsApi.copy(autoSpeak = enabled)))
    }

    fun deleteCharacter(characterId: String, onDeleted: () -> Unit = {}) = action {
        voice.stop()
        val current = state.value.data
        val removedCard = current.characters.find { it.id == characterId }
        val updated = current.copy(
            characters = current.characters.filterNot { it.id == characterId },
            conversations = current.conversations.filterNot { it.character.id == characterId },
        )
        persist(updated)
        removedCard?.let { removeIfUnreferenced(updated, it.wallpaperFile, it.avatarFile) }
        mutable.update { it.copy(notice = "已删除人物卡及相关对话。") }
        onDeleted()
    }

    fun deleteMessage(conversationId: String, messageId: String) = action {
        voice.stop()
        finishMessageAnimation(messageId)
        val current = state.value.data
        persist(current.copy(conversations = current.conversations.map {
            if (it.id == conversationId) it.deleteMessage(messageId) else it
        }))
        mutable.update { it.copy(notice = "已删除该句对话。") }
    }

    fun rewindToMessage(conversationId: String, messageId: String) = action {
        voice.stop()
        finishMessageAnimation()
        val current = state.value.data
        persist(current.copy(conversations = current.conversations.map {
            if (it.id == conversationId) it.rewindTo(messageId) else it
        }))
        mutable.update { it.copy(notice = "已回溯到该句，可继续对话。") }
    }

    fun resetConversation(conversationId: String) = action {
        voice.stop()
        finishMessageAnimation()
        val current = state.value.data
        persist(current.copy(conversations = current.conversations.map {
            if (it.id == conversationId) it.resetToGreeting() else it
        }))
        mutable.update { it.copy(notice = "已回溯人物到初始开场白。") }
    }

    fun updateConversationVoice(conversationId: String, voice: VoiceConfig, syncCard: Boolean, onSaved: () -> Unit) = action {
        require(voice.provider != VoiceProvider.CLOUD || voice.voiceId.isNotBlank()) { "请先选择或填写云端音色 ID。" }
        val current = state.value.data
        val target = current.conversations.find { it.id == conversationId } ?: return@action
        val updatedConversations = current.conversations.map {
            if (it.id == conversationId) it.copy(character = it.character.copy(voice = voice)) else it
        }
        val updatedCharacters = if (syncCard) current.characters.map {
            if (it.id == target.character.id) it.copy(voice = voice, updatedAt = System.currentTimeMillis()) else it
        } else current.characters
        persist(current.copy(conversations = updatedConversations, characters = updatedCharacters))
        mutable.update { it.copy(notice = if (syncCard) "已更新本对话与人物卡声音。" else "已更新本对话声音。") }
        onSaved()
    }

    private fun isMediaReferenced(data: AppData, fileName: String): Boolean {
        if (fileName.isBlank()) return true
        return data.appearance.wallpaperFile == fileName ||
            data.characters.any { it.wallpaperFile == fileName || it.avatarFile == fileName } ||
            data.conversations.any { it.character.wallpaperFile == fileName || it.character.avatarFile == fileName }
    }

    private suspend fun removeIfUnreferenced(data: AppData, vararg candidates: String) {
        for (name in candidates.distinct()) {
            if (name.isNotBlank() && !isMediaReferenced(data, name)) wallpapers.remove(name)
        }
    }

    fun importCharacterWallpaper(characterId: String, uri: Uri) = action {
        withContext(NonCancellable) {
            val current = state.value.data
            val previous = current.characters.find { it.id == characterId }?.wallpaperFile.orEmpty()
            val imported = wallpapers.import(uri)
            val updated = current.copy(
                characters = current.characters.map { if (it.id == characterId) it.copy(wallpaperFile = imported) else it },
                conversations = current.conversations.map {
                    if (it.character.id == characterId) it.copy(character = it.character.copy(wallpaperFile = imported)) else it
                },
            )
            try {
                persist(updated)
            } catch (e: Exception) {
                wallpapers.remove(imported)
                throw e
            }
            removeIfUnreferenced(updated, previous)
            mutable.update { it.copy(notice = "已保存该角色的专属壁纸。") }
        }
    }

    fun resetCharacterWallpaper(characterId: String) = action {
        withContext(NonCancellable) {
            val current = state.value.data
            val previous = current.characters.find { it.id == characterId }?.wallpaperFile.orEmpty()
            val updated = current.copy(
                characters = current.characters.map { if (it.id == characterId) it.copy(wallpaperFile = "") else it },
                conversations = current.conversations.map {
                    if (it.character.id == characterId) it.copy(character = it.character.copy(wallpaperFile = "")) else it
                },
            )
            persist(updated)
            removeIfUnreferenced(updated, previous)
            mutable.update { it.copy(notice = "已恢复默认壁纸。") }
        }
    }

    fun importCharacterAvatar(characterId: String, uri: Uri, onImported: (String) -> Unit = {}) = action {
        withContext(NonCancellable) {
            val current = state.value.data
            val existingCard = current.characters.find { it.id == characterId }
            val previous = existingCard?.avatarFile.orEmpty()
            val imported = wallpapers.import(uri)
            if (existingCard != null) {
                val updated = current.copy(
                    characters = current.characters.map { if (it.id == characterId) it.copy(avatarFile = imported) else it },
                    conversations = current.conversations.map {
                        if (it.character.id == characterId) it.copy(character = it.character.copy(avatarFile = imported)) else it
                    },
                )
                try {
                    persist(updated)
                } catch (e: Exception) {
                    wallpapers.remove(imported)
                    throw e
                }
                removeIfUnreferenced(updated, previous)
            }
            onImported(imported)
            mutable.update { it.copy(notice = "已更新角色自定义头像。") }
        }
    }

    fun resetCharacterAvatar(characterId: String, onReset: () -> Unit = {}) = action {
        withContext(NonCancellable) {
            val current = state.value.data
            val previous = current.characters.find { it.id == characterId }?.avatarFile.orEmpty()
            val updated = current.copy(
                characters = current.characters.map { if (it.id == characterId) it.copy(avatarFile = "") else it },
                conversations = current.conversations.map {
                    if (it.character.id == characterId) it.copy(character = it.character.copy(avatarFile = "")) else it
                },
            )
            persist(updated)
            removeIfUnreferenced(updated, previous)
            onReset()
            mutable.update { it.copy(notice = "头像已恢复默认跟随壁纸。") }
        }
    }

    fun saveCharacter(card: CharacterCard, onSaved: () -> Unit) = action {
        require(card.name.isNotBlank()) { "请填写角色名字。" }
        require(card.persona.isNotBlank()) { "请填写角色设定。" }
        val normalized = card.copy(name = card.name.trim(), updatedAt = System.currentTimeMillis())
        persist(state.value.data.copy(characters = state.value.data.characters.filterNot { it.id == card.id } + normalized))
        onSaved()
    }

    fun openConversation(card: CharacterCard, fresh: Boolean = false, onOpen: (String) -> Unit) = action {
        val existing = state.value.data.conversations.lastOrNull { it.character.id == card.id }
        if (!fresh && existing != null) { onOpen(existing.id); return@action }
        val chat = Conversation(character = card, messages = if (card.greeting.isBlank()) emptyList()
            else listOf(Message(role = Role.ASSISTANT, content = card.greeting)))
        persist(state.value.data.copy(conversations = state.value.data.conversations + chat))
        onOpen(chat.id)
    }

    fun saveApi(config: ApiConfig, replacementKey: String, deleteKey: Boolean, onSaved: () -> Unit) = action {
        ChatClient.endpoint(config.baseUrl)
        require(config.model.isNotBlank()) { "请填写模型 ID。" }
        // Configuration is committed first; key write failure is reported for a retry.
        persist(state.value.data.copy(api = config.copy(baseUrl = config.baseUrl.trim(), model = config.model.trim())))
        withContext(Dispatchers.IO) {
            if (deleteKey) vault.write("") else if (replacementKey.isNotBlank()) vault.write(replacementKey)
        }
        val hasKey = withContext(Dispatchers.IO) { vault.read().isNotBlank() }
        mutable.update { it.copy(hasKey = hasKey, notice = "设置已保存。") }
        onSaved()
    }

    fun saveTtsApi(config: TtsApiConfig, replacementKey: String, deleteKey: Boolean, onSaved: () -> Unit) = action {
        MiniMaxClient.validateEndpoint(config.endpoint)
        require(config.model.isNotBlank()) { "请填写语音模型。" }
        persist(state.value.data.copy(ttsApi = config.copy(endpoint = config.endpoint.trim(), model = config.model.trim())))
        withContext(Dispatchers.IO) {
            if (deleteKey) ttsVault.write("") else if (replacementKey.isNotBlank()) ttsVault.write(replacementKey)
        }
        val hasKey = withContext(Dispatchers.IO) { ttsVault.read().isNotBlank() }
        mutable.update { it.copy(hasTtsKey = hasKey, notice = "云端语音设置已保存。") }
        onSaved()
    }

    fun send(id: String, text: String, retry: Boolean = false, onStored: () -> Unit = {}) {
        if (!available()) return
        val data = state.value.data
        val original = data.conversations.find { it.id == id } ?: return
        val pending = original.messages.lastOrNull()?.role == Role.USER
        if ((!retry && (text.isBlank() || pending)) || (retry && !pending)) return
        try {
            ChatClient.endpoint(data.api.baseUrl)
            require(data.api.model.isNotBlank()) { "请先填写模型 ID。" }
            require(state.value.hasKey) { "请先在接口设置中保存 API Key。" }
        } catch (e: Exception) { mutable.update { it.copy(notice = e.message) }; return }
        mutable.update { it.copy(busy = true, sending = true, notice = null, apiFailure = null, animatingMessageId = null) }
        requestJob = viewModelScope.launch {
            try {
                val chat = if (retry) original else original.copy(messages = original.messages + Message(role = Role.USER, content = text.trim()))
                if (!retry) {
                    persist(data.copy(conversations = data.conversations.map { if (it.id == id) chat else it }))
                    onStored()
                }
                val key = withContext(Dispatchers.IO) { vault.read() }
                val answer = client.complete(data.api, key, PromptBuilder.build(chat, data.api.contextTurns))
                val reply = Message(role = Role.ASSISTANT, content = answer)
                val updated = chat.copy(messages = chat.messages + reply)
                persist(state.value.data.copy(conversations = state.value.data.conversations.map { if (it.id == id) updated else it }))
                mutable.update { it.copy(animatingMessageId = reply.id) }
                if (state.value.data.ttsApi.autoSpeak && answer.isNotBlank()) {
                    voice.speak(answer, updated.character.voice, tag = reply.id)
                }
            } catch (e: CancellationException) {
                mutable.update { it.copy(notice = "已停止。本条消息保留，可重试或撤回。") }
                throw e
            } catch (e: ApiException) {
                mutable.update { it.copy(apiFailure = e.failure) }
            } catch (e: Exception) {
                mutable.update { it.copy(notice = e.message ?: "发送失败，可重试或撤回。") }
            } finally {
                mutable.update { it.copy(busy = false, sending = false) }
            }
        }
    }

    fun clearSuggestions() {
        suggestionJob?.cancel()
        mutable.update { it.copy(suggestionsMessageId = null, suggestions = emptyList(), loadingSuggestions = false) }
    }

    fun requestSuggestions(conversationId: String, messageId: String) {
        val current = state.value
        if (!current.loaded || current.fatal) return
        if (current.suggestionsMessageId == messageId && current.suggestions.isNotEmpty() && !current.loadingSuggestions) {
            clearSuggestions()
            return
        }
        val chat = current.data.conversations.find { it.id == conversationId } ?: return
        val upTo = chat.rewindTo(messageId)
        try {
            ChatClient.endpoint(current.data.api.baseUrl)
            require(current.data.api.model.isNotBlank()) { "请先在接口设置中填写模型 ID。" }
            require(current.hasKey) { "请先在接口设置中保存 API Key。" }
        } catch (e: Exception) {
            mutable.update { it.copy(notice = e.message) }
            return
        }
        suggestionJob?.cancel()
        mutable.update { it.copy(suggestionsMessageId = messageId, suggestions = emptyList(), loadingSuggestions = true, notice = null) }
        suggestionJob = viewModelScope.launch {
            try {
                val key = withContext(Dispatchers.IO) { vault.read() }
                val prompt = PromptBuilder.build(
                    upTo.copy(messages = upTo.messages + Message(
                        role = Role.USER,
                        content = "请站在我的角度，针对角色刚才这句话，给出3条自然、简短、不同方向的中文回复建议。每条占一行，不要编号、不要解释、不要加引号，可以适当包含简短括号动作。",
                    )),
                    current.data.api.contextTurns.coerceAtMost(6),
                )
                val raw = client.complete(current.data.api, key, prompt)
                val parsed = raw.lines()
                    .map { it.trim().replace(Regex("""^(?:[1-9][.、)）]\s*|[-•·]\s*)"""), "").trim('"', '“', '”', '\'') }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(3)
                if (parsed.isEmpty()) {
                    mutable.update { it.copy(loadingSuggestions = false, suggestionsMessageId = null, notice = "暂未生成回复提示，请重试。") }
                } else {
                    mutable.update { it.copy(loadingSuggestions = false, suggestions = parsed) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                mutable.update { it.copy(loadingSuggestions = false, suggestionsMessageId = null, apiFailure = e.failure) }
            } catch (e: Exception) {
                mutable.update { it.copy(loadingSuggestions = false, suggestionsMessageId = null, notice = e.message ?: "生成回复提示失败。") }
            }
        }
    }

    fun cancelRequest() { requestJob?.cancel() }
    fun discardPending(id: String) = action {
        val data = state.value.data
        persist(data.copy(conversations = data.conversations.map {
            if (it.id == id && it.messages.lastOrNull()?.role == Role.USER) it.copy(messages = it.messages.dropLast(1)) else it
        }))
    }

    override fun onCleared() { voice.close(); super.onCleared() }
}
