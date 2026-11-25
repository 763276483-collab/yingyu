package com.rolechat.app

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rolechat.app.domain.*
import com.rolechat.app.appearance.*
import com.rolechat.app.network.CloudVoicePreset
import com.rolechat.app.network.MiniMaxClient
import com.rolechat.app.ui.*
import com.rolechat.app.voice.VoiceOption
import com.rolechat.app.voice.VoiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

private val YingYuAccentBrush = Brush.horizontalGradient(
    listOf(Color(0xFF8B6BFF), Color(0xFF6AA4FF))
)

@Composable
private fun CharacterAvatar(
    name: String,
    imageFile: String,
    wallpapers: WallpaperStore,
    size: Dp,
) {
    val bitmap by produceState<ImageBitmap?>(null, imageFile) {
        value = if (imageFile.isBlank()) null else runCatching {
            withContext(Dispatchers.IO) { wallpapers.load(imageFile).asImageBitmap() }
        }.getOrNull()
    }
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(YingYuAccentBrush),
        contentAlignment = Alignment.Center,
    ) {
        val img = bitmap
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                name.trim().take(1).ifEmpty { "语" },
                color = Color.White,
                style = if (size >= 40.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scene = LocalNightGlass.current
    if (scene != null) {
        GlassSurface(scene.backdrop, scene.appearance, modifier = modifier.fillMaxWidth(),
            tint = Color(0xFF11182C), shape = RoundedCornerShape(24.dp), borderAlpha = 0.32f) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
        }
    } else Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = YingYuNightColors) { RoleChatApp() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleChatApp(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val speech by vm.voice.state.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf("cards") }
    var editorId by rememberSaveable { mutableStateOf("") }
    var chatId by rememberSaveable { mutableStateOf("") }
    val snackbars = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val view = LocalView.current
    val lightSystemBars = route == "chat" && state.data.appearance.isDarkTextColor()
    SideEffect {
        val window = (view.context as? ComponentActivity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightSystemBars
            isAppearanceLightNavigationBars = lightSystemBars
        }
    }
    state.apiFailure?.let { failure ->
        AlertDialog(onDismissRequest = vm::clearApiFailure, title = { Text(failure.title) },
            text = { SelectionContainer {
                Text(failure.details, Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()))
            } },
            confirmButton = { TextButton(onClick = vm::clearApiFailure) { Text("关闭") } },
            dismissButton = { TextButton(onClick = { clipboard.setText(AnnotatedString("${failure.title}\n${failure.details}")) }) { Text("复制详情") } })
    }
    LaunchedEffect(state.notice) {
        state.notice?.let { snackbars.showSnackbar(it); vm.clearNotice() }
    }
    BackHandler(route != "cards") { vm.voice.stop(); vm.finishMessageAnimation(); route = "cards" }
    NightScene(vm.wallpapers, enabled = route != "chat") {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = if (route == "chat") WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        topBar = { if (route != "chat") TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (route == "cards") {
                    Image(
                        painter = painterResource(R.drawable.ic_brand_logo),
                        contentDescription = "映语",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)),
                    )
                }
                Column {
                    Text(
                        when (route) { "edit" -> "人物卡"; "chat" -> "角色对话"; "settings" -> "模型接口"; else -> "映语" },
                        fontWeight = FontWeight.Bold,
                    )
                    if (route == "cards") Text("映照心声，与每个灵魂对话", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } },
            navigationIcon = {
                if (route != "cards") IconButton(onClick = { vm.voice.stop(); vm.finishMessageAnimation(); route = "cards" }) {
                    BackArrowIcon(MaterialTheme.colorScheme.onSurface, 22.dp)
                }
            },
            actions = {
                if (state.sending && route != "chat") TextButton(onClick = vm::cancelRequest) { Text("停止生成") }
                if (route == "cards") Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(end = 10.dp).bounceClick(enabled = !state.busy) { route = "settings" },
                ) {
                    Text(
                        "接口设置",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            },
        ) },
        snackbarHost = { SnackbarHost(snackbars) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.fatal -> Text("无法读取本地数据。为保护聊天记录，已停止写入。请关闭应用后重试；若仍有问题，请保留应用数据并联系开发者。", Modifier.padding(24.dp))
                else -> AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        if (targetState != "cards") {
                            (slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { -it / 8 } + fadeOut(tween(180)))
                        } else {
                            (slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { -it / 6 } + fadeIn(tween(200))) togetherWith
                                (slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { it / 5 } + fadeOut(tween(180)))
                        }
                    },
                    label = "routeTransition",
                ) { activeRoute ->
                    when (activeRoute) {
                        "edit" -> {
                            val card = state.data.characters.find { it.id == editorId }
                            key(editorId) { CharacterEditor(card, speech.voices, state.availableCloudVoices, state.busy,
                                wallpapers = vm.wallpapers,
                                globalWallpaper = state.data.appearance.wallpaperFile,
                                onImportAvatar = { id, uri, done -> vm.importCharacterAvatar(id, uri, done) },
                                onResetAvatar = { id, done -> vm.resetCharacterAvatar(id, done) },
                                onFetchCloudVoices = vm::fetchCloudVoices,
                                onSave = { vm.saveCharacter(it) { route = "cards" } },
                                onPreview = { vm.voice.speak("你好，很高兴认识你。今天想聊些什么？", it) },
                                speechStatus = speech.status) }
                        }
                        "settings" -> ApiSettings(state,
                            onTest = vm::testApi,
                            onSave = { config, key, remove, saved -> vm.saveApi(config, key, remove, saved) },
                            onSaveTts = { config, key, remove, saved -> vm.saveTtsApi(config, key, remove, saved) })
                        "chat" -> {
                            val chat = state.data.conversations.find { it.id == chatId }
                            if (chat == null) Text("这段对话不存在，请返回人物列表。", Modifier.padding(24.dp))
                            else ChatScreen(chat, state, speech,
                                wallpapers = vm.wallpapers,
                                onBack = { vm.voice.stop(); vm.finishMessageAnimation(); route = "cards" },
                                onSaveAppearance = vm::saveAppearance,
                                onWallpaper = { uri -> vm.importCharacterWallpaper(chat.character.id, uri) },
                                onResetWallpaper = { vm.resetCharacterWallpaper(chat.character.id) },
                                onFetchCloudVoices = vm::fetchCloudVoices,
                                onUpdateVoice = { v, sync, done -> vm.updateConversationVoice(chat.id, v, sync, done) },
                                onPreviewVoice = { vm.voice.speak("你好，很高兴认识你。今天想聊些什么？", it) },
                                onSetAutoSpeak = vm::setAutoSpeak,
                                onDeleteMessage = { msgId -> vm.deleteMessage(chat.id, msgId) },
                                onRewindTo = { msgId -> vm.rewindToMessage(chat.id, msgId) },
                                onResetConversation = { vm.resetConversation(chat.id) },
                                onRequestSuggestions = { msgId -> vm.requestSuggestions(chat.id, msgId) },
                                onClearSuggestions = vm::clearSuggestions,
                                onFinishAnimation = { msgId -> vm.finishMessageAnimation(msgId) },
                                onSend = { text, stored -> vm.clearSuggestions(); vm.send(chat.id, text, onStored = stored) },
                                onRetry = { vm.send(chat.id, "", retry = true) },
                                onDiscard = { vm.discardPending(chat.id) },
                                onCancel = vm::cancelRequest,
                                onSpeak = { text, msgId -> vm.voice.speak(text, chat.character.voice, tag = msgId) },
                                onStop = vm.voice::stop,
                                onOpen = { vm.voice.stop(); vm.finishMessageAnimation(); chatId = it },
                                onNew = {
                                    val current = state.data.characters.find { it.id == chat.character.id } ?: chat.character
                                    vm.openConversation(current, fresh = true) { vm.voice.stop(); vm.finishMessageAnimation(); chatId = it }
                                })
                        }
                        else -> CharacterList(state, vm.wallpapers, onCreate = { editorId = ""; route = "edit" },
                            onEdit = { editorId = it.id; route = "edit" },
                            onDelete = { vm.deleteCharacter(it.id) },
                            onChat = { vm.openConversation(it) { id -> chatId = id; route = "chat" } })
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun CharacterList(
    state: AppState,
    wallpapers: WallpaperStore,
    onCreate: () -> Unit,
    onEdit: (CharacterCard) -> Unit,
    onDelete: (CharacterCard) -> Unit,
    onChat: (CharacterCard) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<CharacterCard?>(null) }
    val haptic = LocalHapticFeedback.current
    val shimmerBrush = rememberShimmerAccentBrush()
    pendingDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除人物卡") },
            text = { Text("确定删除「${card.name}」吗？该角色的聊天记录也会一并删除，此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete(card)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(key = "home-header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("我的人物", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.06f)) {
                    Text("${state.data.characters.size} 位角色", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("选择一位角色，继续你们的故事。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(if (!state.busy) shimmerBrush else Brush.horizontalGradient(listOf(Color(0xFFC9C2DE), Color(0xFFC9C2DE))))
                    .bounceClick(enabled = !state.busy, pressedScale = 0.96f, onClick = onCreate)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("＋ 创建人物卡", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
        if (state.data.characters.isEmpty()) item(key = "home-empty") {
            StaggeredEntrance(index = 0) {
                SectionCard {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_brand_logo),
                            contentDescription = "映语",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(68.dp).clip(RoundedCornerShape(20.dp)),
                        )
                        Text("第一个角色，由你来定义", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "人物卡包含名字、角色设定、开场白、简介和声音。配置模型接口后，就能开始聊天。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        itemsIndexed(state.data.characters.sortedByDescending { it.updatedAt }, key = { _, card -> card.id }) { index, card ->
            StaggeredEntrance(index = index, modifier = Modifier.animateItem()) {
                SectionCard(
                    modifier = Modifier.bounceClick(enabled = !state.busy, pressedScale = 0.98f) { onChat(card) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CharacterAvatar(
                            name = card.name,
                            imageFile = card.effectiveAvatar(state.data.appearance.wallpaperFile),
                            wallpapers = wallpapers,
                            size = 48.dp,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(card.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(card.description.ifBlank { "还没有简介" }, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                if (card.voice.provider == VoiceProvider.CLOUD) "云端语音" else "系统语音",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                    if (card.greeting.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.045f)) {
                            Text("“${card.greeting.take(100)}”", maxLines = 3, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.bounceClick(enabled = !state.busy) { onChat(card) },
                        ) {
                            Text("开始聊天", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge,
                                maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.bounceClick(enabled = !state.busy) { onEdit(card) },
                        ) {
                            Text("编辑", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelLarge,
                                maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            modifier = Modifier.bounceClick(enabled = !state.busy) { pendingDelete = card },
                        ) {
                            Text("删除", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge,
                                maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceTuningSection(
    voice: VoiceConfig,
    onChange: (VoiceConfig) -> Unit,
    systemVoices: List<VoiceOption>,
    cloudVoices: List<CloudVoicePreset>,
    busy: Boolean,
    onFetchCloudVoices: () -> Unit,
    onPreview: (VoiceConfig) -> Unit,
    speechStatus: String,
) {
    val cloud = voice.provider == VoiceProvider.CLOUD
    var showCloudMenu by remember { mutableStateOf(false) }
    var showSystemMenu by remember { mutableStateOf(false) }
    val allCloudOptions = remember(cloudVoices) {
        val ids = cloudVoices.map { it.id }.toSet()
        cloudVoices + MiniMaxClient.PRESET_VOICES.filterNot { it.id in ids }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(selected = cloud, onClick = {
                val id = if (voice.provider == VoiceProvider.CLOUD) voice.voiceId else "male-qn-qingse"
                onChange(voice.copy(provider = VoiceProvider.CLOUD, voiceId = id.ifBlank { "male-qn-qingse" }))
            }, label = { Text("云端语音") }, enabled = !busy)
            FilterChip(selected = !cloud, onClick = {
                val id = if (voice.provider == VoiceProvider.SYSTEM) voice.voiceId else ""
                onChange(voice.copy(provider = VoiceProvider.SYSTEM, voiceId = id))
            }, label = { Text("系统语音") }, enabled = !busy)
        }
        if (cloud) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    OutlinedButton(onClick = { showCloudMenu = true }, enabled = !busy) {
                        val currentLabel = allCloudOptions.find { it.id == voice.voiceId }?.label ?: "选择音色预设"
                        Text(currentLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(showCloudMenu, { showCloudMenu = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                        allCloudOptions.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text("${preset.label} (${preset.id})") },
                                onClick = { onChange(voice.copy(voiceId = preset.id)); showCloudMenu = false },
                            )
                        }
                    }
                }
                OutlinedButton(onClick = onFetchCloudVoices, enabled = !busy) { Text("拉取账号音色") }
            }
            OutlinedTextField(voice.voiceId, { onChange(voice.copy(voiceId = it)) }, label = { Text("MiniMax 音色 ID（可直接选择或手动填写）") },
                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
            Text("语速：${"%.1f".format(voice.speed)}")
            Slider(voice.speed, { onChange(voice.copy(speed = it)) }, valueRange = 0.5f..2f, enabled = !busy)
            Text("音调：${if (voice.cloudPitch > 0) "+${voice.cloudPitch}" else "${voice.cloudPitch}"} 半音")
            Slider(voice.cloudPitch.toFloat(), { onChange(voice.copy(cloudPitch = it.roundToInt())) }, valueRange = -12f..12f, steps = 23, enabled = !busy)
            Text("音色明暗（低沉 ↔ 清亮）：${voice.timbre}")
            Slider(voice.timbre.toFloat(), { onChange(voice.copy(timbre = it.roundToInt())) }, valueRange = -100f..100f, enabled = !busy)
            Text("声音强弱（柔和 ↔ 浑厚）：${voice.intensity}")
            Slider(voice.intensity.toFloat(), { onChange(voice.copy(intensity = it.roundToInt())) }, valueRange = -100f..100f, enabled = !busy)
            Text("情绪风格", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniMaxClient.EMOTION_OPTIONS.forEach { (code, label) ->
                    FilterChip(selected = voice.emotion == code, onClick = { onChange(voice.copy(emotion = code)) },
                        label = { Text(label) }, enabled = !busy)
                }
            }
            Text("试听和朗读会将文本发送给语音服务商，并可能产生费用。", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("可在手机系统设置中切换语音引擎。", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(voice.offlineOnly, { onChange(voice.copy(offlineOnly = it, voiceId = "")) }, enabled = !busy)
                Text("只使用离线音色")
            }
            Box {
                OutlinedButton(onClick = { showSystemMenu = true }, enabled = !busy) {
                    Text(systemVoices.find { it.id == voice.voiceId }?.label ?: if (voice.voiceId.isBlank()) "自动选择中文音色" else "已保存音色：${voice.voiceId}", maxLines = 2)
                }
                DropdownMenu(showSystemMenu, { showSystemMenu = false }, modifier = Modifier.heightIn(max = 300.dp)) {
                    DropdownMenuItem(text = { Text("自动选择中文音色") }, onClick = { onChange(voice.copy(voiceId = "")); showSystemMenu = false })
                    systemVoices.filter { !voice.offlineOnly || it.offline }.forEach { item ->
                        DropdownMenuItem(text = { Text("${item.label} · ${if (item.offline) "离线" else "联网"}") },
                            onClick = { onChange(voice.copy(voiceId = item.id)); showSystemMenu = false })
                    }
                }
            }
            Text("语速：${"%.1f".format(voice.speed)}")
            Slider(voice.speed, { onChange(voice.copy(speed = it)) }, valueRange = 0.5f..2f, enabled = !busy)
            Text("音调：${"%.1f".format(voice.pitch)}")
            Slider(voice.pitch, { onChange(voice.copy(pitch = it)) }, valueRange = 0.5f..2f, enabled = !busy)
        }
        OutlinedButton(onClick = { onPreview(voice) }, enabled = !busy) { Text("试听声音") }
        Text(speechStatus, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CharacterEditor(
    original: CharacterCard?, voices: List<VoiceOption>, cloudVoices: List<CloudVoicePreset>, busy: Boolean,
    wallpapers: WallpaperStore,
    globalWallpaper: String,
    onImportAvatar: (String, Uri, (String) -> Unit) -> Unit,
    onResetAvatar: (String, () -> Unit) -> Unit,
    onFetchCloudVoices: () -> Unit,
    onSave: (CharacterCard) -> Unit, onPreview: (VoiceConfig) -> Unit, speechStatus: String,
) {
    val id = rememberSaveable { original?.id ?: newId() }
    var name by rememberSaveable { mutableStateOf(original?.name.orEmpty()) }
    var persona by rememberSaveable { mutableStateOf(original?.persona.orEmpty()) }
    var greeting by rememberSaveable { mutableStateOf(original?.greeting.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(original?.description.orEmpty()) }
    var voice by remember(original?.id) { mutableStateOf(original?.voice ?: VoiceConfig()) }
    var avatarFile by rememberSaveable(original?.id) { mutableStateOf(original?.avatarFile.orEmpty()) }
    val wallpaperFile = original?.wallpaperFile.orEmpty()
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportAvatar(id, uri) { avatarFile = it }
    }
    val cloud = voice.provider == VoiceProvider.CLOUD
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard {
            Text("角色档案", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CharacterAvatar(
                    name = name,
                    imageFile = avatarFile.ifBlank { wallpaperFile.ifBlank { globalWallpaper } },
                    wallpapers = wallpapers,
                    size = 64.dp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (avatarFile.isNotBlank()) "已设置自定义头像" else "默认使用角色壁纸作为头像",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { avatarPicker.launch(arrayOf("image/*")) }, enabled = !busy, shape = RoundedCornerShape(12.dp)) {
                            Text("自定义头像")
                        }
                        if (avatarFile.isNotBlank()) {
                            TextButton(onClick = { onResetAvatar(id) { avatarFile = "" } }, enabled = !busy) { Text("恢复默认") }
                        }
                    }
                }
            }
            OutlinedTextField(name, { name = it }, label = { Text("角色名字 *") }, singleLine = true, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("角色简介") }, minLines = 2, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            Text("简介用于人物列表展示；角色设定会发送给聊天模型。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(persona, { persona = it }, label = { Text("角色设定 *") }, placeholder = { Text("性格、背景、说话方式，以及角色与你的关系…") }, minLines = 5, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(greeting, { greeting = it }, label = { Text("开场白") }, minLines = 3, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        }
        SectionCard {
            Text("角色声音（此人物卡独立保存）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            VoiceTuningSection(voice, { voice = it }, voices, cloudVoices, busy, onFetchCloudVoices, onPreview, speechStatus)
        }
        if (original != null) Text("修改人物卡后，新对话使用新设定；已有对话也可在聊天页顶部「声音」直接调整。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { onSave(CharacterCard(id, name, persona, greeting, description, voice, wallpaperFile = wallpaperFile, avatarFile = avatarFile)) },
            enabled = !busy && name.isNotBlank() && persona.isNotBlank() && (!cloud || voice.voiceId.isNotBlank()),
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text(if (busy) "正在保存…" else "保存人物卡")
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ApiSettings(state: AppState, onSave: (ApiConfig, String, Boolean, () -> Unit) -> Unit,
    onSaveTts: (TtsApiConfig, String, Boolean, () -> Unit) -> Unit,
    onTest: (ApiConfig, String) -> Unit) {
    var baseUrl by rememberSaveable { mutableStateOf(state.data.api.baseUrl) }
    var model by rememberSaveable { mutableStateOf(state.data.api.model) }
    // Keys deliberately stay out of saved UI state and clipboard helpers.
    var key by remember { mutableStateOf("") }
    var remove by remember { mutableStateOf(false) }
    var turns by rememberSaveable { mutableFloatStateOf(state.data.api.contextTurns.toFloat()) }
    var ttsEndpoint by rememberSaveable { mutableStateOf(state.data.ttsApi.endpoint) }
    var ttsModel by rememberSaveable { mutableStateOf(state.data.ttsApi.model) }
    var ttsKey by remember { mutableStateOf("") }
    var removeTtsKey by remember { mutableStateOf(false) }
    var showModels by remember { mutableStateOf(false) }
    val models = if (state.modelsBaseUrl == baseUrl.trim()) state.availableModels else emptyList()
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard {
            Text("让角色会聊天，也有自己的声音", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("推荐组合：DeepSeek 生成对话文字，MiniMax 把角色台词变成声音。先完成文字聊天，再按需要开通语音。",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("API 是映语连接模型服务的方式。你需要在对应的官方开放平台创建 API Key（接口密钥），再填入下面的设置。两家服务分别开通、分别计费，额度和可用模型以各自平台为准。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionCard {
            Text("01 · 文字聊天 / DeepSeek", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            SetupStep(1, "在官网创建聊天密钥", "打开 DeepSeek 开放平台，注册或登录，进入 API keys 创建并复制密钥。请确认账号有可用 API 额度。")
            OfficialApiLink("DeepSeek 官方开放平台 · 获取 Key", "https://platform.deepseek.com/")
            OfficialApiLink("DeepSeek 官方接入文档", "https://api-docs.deepseek.com/zh-cn/")
            SetupStep(2, "填入地址和 Key", "点击官方配置按钮，再把刚复制的 DeepSeek Key 粘贴到聊天密钥框。基础地址是接口域名，不是上面的平台官网网址。")
            OutlinedButton(onClick = { baseUrl = "https://api.deepseek.com"; model = "deepseek-flash" }, enabled = !state.busy,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Text("填入 DeepSeek 官方配置") }
            OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("聊天 API 基础地址") },
                placeholder = { Text("https://api.deepseek.com") },
                supportingText = { Text("DeepSeek 填 https://api.deepseek.com；不要填写平台官网，也不要追加 /chat/completions。其他服务需兼容 OpenAI Chat Completions。") },
                singleLine = true, enabled = !state.busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(key, { key = it }, label = { Text(if (state.hasKey) "更换聊天 Key（留空保留）" else "DeepSeek 聊天 API Key") },
                supportingText = { Text("只粘贴密钥本身，不加 Bearer 或引号。密钥会加密保存在本机。") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !state.busy && !remove,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            Text(if (state.hasKey) "已保存加密聊天密钥；留空可继续使用" else "尚未保存聊天密钥",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SetupStep(3, "获取并选择可用模型", "先测试连接，再从返回列表中选择。日常角色聊天可先用 deepseek-flash；其他模型只有在列表中可用时再选择，能力与价格可查看官网文档。模型 ID 是服务商的模型名称，不是人物名字。")
            OutlinedButton(onClick = { onTest(ApiConfig(baseUrl, model, turns.toInt()), key) },
                enabled = !state.busy && baseUrl.isNotBlank() && !remove && (state.hasKey || key.isNotBlank()),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text(if (state.busy) "正在处理…" else "测试连接并获取模型")
            }
            Text("这里只读取模型列表，不发送聊天内容，也不会自动保存。Key 留空时使用已保存的聊天密钥；列表成功后仍需保存并实际发送一句话确认可用。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (models.isNotEmpty()) Box {
                OutlinedButton(onClick = { showModels = true }, enabled = !state.busy, shape = RoundedCornerShape(14.dp)) {
                    Text("选择模型 · ${models.size} 个可用")
                }
                DropdownMenu(showModels, { showModels = false }, modifier = Modifier.heightIn(max = 300.dp)) {
                    models.forEach { id -> DropdownMenuItem(text = { Text(id) }, onClick = { model = id; showModels = false }) }
                }
            }
            OutlinedTextField(model, { model = it }, label = { Text("聊天模型 ID") },
                supportingText = { Text("推荐从获取到的模型列表选择；也可填写官方文档中的准确 ID，例如 deepseek-flash。") },
                singleLine = true, enabled = !state.busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            if (baseUrl.trim().trimEnd('/') in listOf("https://api.deepseek.com", "https://api.deepseek.com/v1")) {
                Text("映语的 DeepSeek 对话使用普通聊天模式，当前关闭深度思考。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.hasKey) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(remove, { remove = it }, enabled = !state.busy)
                Text("保存时删除现有聊天密钥", modifier = Modifier.weight(1f))
            }
            Text("携带的聊天历史：最近 ${turns.toInt()} 轮")
            Slider(turns, { turns = it }, valueRange = 1f..50f, steps = 48, enabled = !state.busy)
            Text("一轮是你的发言和角色回复。轮数越多，角色能参考的历史越多，发送量也越大；超出模型上下文限制时可降低轮数。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SetupStep(4, "保存，再开始对话", "保存后返回人物列表，创建或打开人物卡，发送一句简短消息测试。若报错，请查看错误详情中的模型、余额或认证提示。")
            Button(onClick = { onSave(ApiConfig(baseUrl, model, turns.toInt()), key, remove) { key = ""; remove = false } },
                enabled = !state.busy && baseUrl.isNotBlank() && model.isNotBlank(), shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("保存聊天接口设置") }
        }
        SectionCard {
            Text("02 · 角色语音 / MiniMax", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("此处负责把角色台词合成为声音。语音模型与聊天模型不同，MiniMax Key 填在这里；DeepSeek Key 填在上面的聊天区域。",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            SetupStep(1, "在对应站点开通语音 API", "国内站账号使用国内接口，国际站账号使用国际接口。登录开放平台，在“接口密钥 / API Keys”创建可用于语音的 Key，并确认有语音服务权限和额度。")
            OfficialApiLink("MiniMax 官方开放平台 · 国内站", "https://platform.minimax.cn/")
            OfficialApiLink("MiniMax 官方开放平台 · 国际站", "https://platform.minimax.io/")
            OfficialApiLink("MiniMax 官方语音接入文档", "https://platform.minimax.cn/docs/api-reference/speech-t2a-http")
            SetupStep(2, "选择站点、语音模型并粘贴 Key", "国内站和国际站的地址应与申请 Key 的站点一致。可先使用默认 speech-2.8-hd，或选择 speech-2.8-turbo；实际可用范围以账号和官方文档为准。")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { ttsEndpoint = "https://api.minimax.cn/v1/t2a_v2" }, enabled = !state.busy,
                    shape = RoundedCornerShape(14.dp)) { Text("填入国内站地址", maxLines = 1) }
                OutlinedButton(onClick = { ttsEndpoint = "https://api.minimax.io/v1/t2a_v2" }, enabled = !state.busy,
                    shape = RoundedCornerShape(14.dp)) { Text("填入国际站地址", maxLines = 1) }
            }
            OutlinedTextField(ttsEndpoint, { ttsEndpoint = it }, label = { Text("语音完整接口地址") },
                supportingText = { Text("与上面的聊天基础地址不同，这里要包含 /v1/t2a_v2。切换站点后也要使用对应站点的 Key。") },
                singleLine = true, enabled = !state.busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = ttsModel == "speech-2.8-hd", onClick = { ttsModel = "speech-2.8-hd" }, enabled = !state.busy,
                    label = { Text("speech-2.8-hd") })
                FilterChip(selected = ttsModel == "speech-2.8-turbo", onClick = { ttsModel = "speech-2.8-turbo" }, enabled = !state.busy,
                    label = { Text("speech-2.8-turbo") })
            }
            OutlinedTextField(ttsModel, { ttsModel = it }, label = { Text("语音模型 ID") },
                supportingText = { Text("模型决定使用哪一种语音合成服务；具体的男声、女声与音色，在人物卡或聊天页“声音”中选择。") },
                singleLine = true, enabled = !state.busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ttsKey, { ttsKey = it }, label = { Text(if (state.hasTtsKey) "更换语音 Key（留空保留）" else "MiniMax 语音 API Key") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !state.busy && !removeTtsKey,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            Text(if (state.hasTtsKey) "已保存独立的加密语音密钥" else "尚未保存语音密钥；暂时不配置也能文字聊天",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.hasTtsKey) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(removeTtsKey, { removeTtsKey = it }, enabled = !state.busy)
                Text("保存时删除现有语音密钥", modifier = Modifier.weight(1f))
            }
            SetupStep(3, "保存后为人物挑选音色", "保存语音设置，再打开人物卡或聊天页“声音”，选择云端语音、拉取账号音色并试听。声音面板可开关自动朗读；试听和朗读会调用 MiniMax，并按平台规则计费。")
            Button(onClick = { onSaveTts(state.data.ttsApi.copy(endpoint = ttsEndpoint, model = ttsModel), ttsKey, removeTtsKey) { ttsKey = ""; removeTtsKey = false } },
                enabled = !state.busy && ttsEndpoint.isNotBlank() && ttsModel.isNotBlank(), shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("保存语音设置") }
        }
    }
}


private val TEXT_COLOR_PRESETS = listOf(
    "#111111" to "纯黑",
    "#142032" to "墨蓝",
    "#FFFFFF" to "纯白",
    "#FFF6E5" to "暖白",
    "#FFE4F2" to "柔粉",
    "#E0F7FF" to "浅青",
    "#3A1F5D" to "深紫",
    "#3E2723" to "深棕",
)

private val NARRATION_SPAN_REGEX = Regex("""（[^）]*(?:）|$)|\([^)]*(?:\)|$)|【[^】]*(?:】|$)""")

private fun formatRoleplayText(raw: String, foreground: Color): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in NARRATION_SPAN_REGEX.findAll(raw)) {
        if (match.range.first > cursor) append(raw.substring(cursor, match.range.first))
        withStyle(SpanStyle(color = foreground.copy(alpha = 0.65f))) { append(match.value) }
        cursor = match.range.last + 1
    }
    if (cursor < raw.length) append(raw.substring(cursor))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatScreen(
    chat: Conversation, state: AppState, speech: VoiceState,
    wallpapers: WallpaperStore, onBack: () -> Unit,
    onSaveAppearance: (ChatAppearance, () -> Unit) -> Unit, onWallpaper: (Uri) -> Unit, onResetWallpaper: () -> Unit,
    onFetchCloudVoices: () -> Unit,
    onUpdateVoice: (VoiceConfig, Boolean, () -> Unit) -> Unit,
    onPreviewVoice: (VoiceConfig) -> Unit,
    onSetAutoSpeak: (Boolean) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onRewindTo: (String) -> Unit,
    onResetConversation: () -> Unit,
    onRequestSuggestions: (String) -> Unit,
    onClearSuggestions: () -> Unit,
    onFinishAnimation: (String) -> Unit,
    onSend: (String, () -> Unit) -> Unit, onRetry: () -> Unit, onDiscard: () -> Unit,
    onCancel: () -> Unit, onSpeak: (String, String) -> Unit, onStop: () -> Unit,
    onOpen: (String) -> Unit, onNew: () -> Unit,
) {
    val systemVoices = speech.voices
    val speechStatus = speech.status
    var draft by rememberSaveable(chat.id) { mutableStateOf("") }
    var showFullHistory by rememberSaveable(chat.id) { mutableStateOf(false) }
    // Progress belongs to the conversation, not a list item that is disposed during a mode switch.
    val revealProgress = remember(chat.id) { mutableStateMapOf<String, Int>() }
    var showHistory by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }
    var showVoice by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var menuMessageId by remember { mutableStateOf<String?>(null) }
    var appearanceDraft by remember { mutableStateOf(state.data.appearance) }
    var customHexInput by remember { mutableStateOf(state.data.appearance.textColorHex) }
    var voiceDraft by remember(chat.id, chat.character.voice) { mutableStateOf(chat.character.voice) }
    var syncVoiceToCard by remember { mutableStateOf(true) }
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val bgAlpha = remember(chat.id) { Animatable(0f) }
    val bgScale = remember(chat.id) { Animatable(1.045f) }
    LaunchedEffect(chat.id) {
        launch { bgAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
        launch { bgScale.animateTo(1f, tween(460, easing = FastOutSlowInEasing)) }
    }
    val effectiveWallpaper = chat.character.effectiveWallpaper(state.data.appearance.wallpaperFile)
    val appearance = (if (showAppearance) appearanceDraft else state.data.appearance).copy(wallpaperFile = effectiveWallpaper)
    val backdrop = rememberGlassBackdrop(wallpapers, appearance)
    val foreground = Color(0xFF000000L or appearance.resolvedTextRgb().toLong())
    val darkText = appearance.isDarkTextColor()
    val glassText = TextStyle(color = foreground, shadow = Shadow(
        color = if (darkText) Color.White.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.6f), blurRadius = 3f))
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) onWallpaper(uri) }
    // Recreate both the list state and its layout when switching modes. Old indices, anchors and
    // animateItem removal layers must not survive a switch from full history to the latest turn.
    val list = key(chat.id, showFullHistory) { rememberLazyListState() }
    val pending = chat.messages.lastOrNull()?.role == Role.USER
    val sendEnabled = draft.isNotBlank() && !state.busy && !pending
    val history = state.data.conversations.filter { it.character.id == chat.character.id }.sortedByDescending { it.createdAt }
    val latestAssistantId = chat.messages.lastOrNull { it.role == Role.ASSISTANT }?.id
    val (focusedMessages, hiddenCount) = remember(chat.messages) { chat.latestTurnWindow() }
    val hasHiddenHistory = hiddenCount > 0
    LaunchedEffect(chat.id, hasHiddenHistory) {
        if (!hasHiddenHistory) showFullHistory = false
    }
    val isFocusMode = !showFullHistory
    val visibleMessages = if (showFullHistory) chat.messages else focusedMessages
    val showIntroCard = chat.character.description.isNotBlank() && showFullHistory
    val totalListItems = (if (showIntroCard) 1 else 0) +
        visibleMessages.size +
        (if (state.sending) 1 else 0)
    val showSpeechPill = speechStatus.isNotBlank() && speechStatus != "朗读已停止。" && speechStatus != "朗读完成。" &&
        !speechStatus.startsWith("云端语音：请先保存语音 Key")
    LaunchedEffect(chat.id, chat.messages.lastOrNull()?.id, state.sending, showFullHistory) {
        if (totalListItems > 0) list.scrollToItem(totalListItems - 1)
    }
    if (showResetConfirm) AlertDialog(
        onDismissRequest = { showResetConfirm = false },
        title = { Text("回溯人物") },
        text = { Text("确定将当前对话回溯到「${chat.character.name}」的初始开场白吗？本段已聊内容将被清空。") },
        confirmButton = {
            TextButton(onClick = { showResetConfirm = false; onResetConversation() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("确认回溯") }
        },
        dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("取消") } },
    )
    if (showVoice) AlertDialog(
        onDismissRequest = { if (!state.busy) showVoice = false },
        title = { Text("角色声音与自动朗读") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("收到回复自动朗读", style = MaterialTheme.typography.titleSmall)
                        Text("默认开启，每句新回复自动朗读；可随时点右上角音量图标静音。", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = state.data.ttsApi.autoSpeak, onCheckedChange = onSetAutoSpeak, enabled = !state.busy)
                }
                HorizontalDivider()
                VoiceTuningSection(voiceDraft, { voiceDraft = it }, systemVoices, state.availableCloudVoices, state.busy,
                    onFetchCloudVoices, onPreviewVoice, speechStatus)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(syncVoiceToCard, { syncVoiceToCard = it }, enabled = !state.busy)
                    Text("同时更新人物卡（影响以后新对话）", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onUpdateVoice(voiceDraft, syncVoiceToCard) { showVoice = false } },
                enabled = !state.busy && (voiceDraft.provider != VoiceProvider.CLOUD || voiceDraft.voiceId.isNotBlank())) { Text("应用声音") }
        },
        dismissButton = { TextButton(onClick = { showVoice = false }, enabled = !state.busy) { Text("取消") } },
    )
    if (showHistory) AlertDialog(onDismissRequest = { showHistory = false }, title = { Text("历史对话") },
        text = { LazyColumn(Modifier.heightIn(max = 400.dp)) { items(history, key = { it.id }) { item ->
            Column(Modifier.fillMaxWidth().clickable(enabled = !state.busy) { onOpen(item.id); showHistory = false }.padding(vertical = 12.dp)) {
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.createdAt)))
                Text(item.messages.firstOrNull { it.role == Role.USER }?.content?.take(45) ?: "新的对话", style = MaterialTheme.typography.bodySmall)
            }
        } } }, confirmButton = { TextButton(onClick = { showHistory = false }) { Text("关闭") } })
    Box(Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = bgAlpha.value; scaleX = bgScale.value; scaleY = bgScale.value }) {
        WallpaperBackground(backdrop)
    }
    CompositionLocalProvider(LocalContentColor provides foreground) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassSurface(backdrop, appearance, shape = RoundedCornerShape(999.dp),
                modifier = Modifier.size(42.dp).bounceClick(onClick = { onClearSuggestions(); onBack() })) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { BackArrowIcon(foreground, 20.dp) }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                GlassSurface(backdrop, appearance, shape = RoundedCornerShape(999.dp)) {
                    Row(
                        Modifier.padding(start = 4.dp, end = 14.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SpeakingAvatarAura(active = speech.speaking, size = 30.dp) {
                            CharacterAvatar(
                                name = chat.character.name,
                                imageFile = chat.character.effectiveAvatar(state.data.appearance.wallpaperFile),
                                wallpapers = wallpapers,
                                size = 30.dp,
                            )
                        }
                        Text(chat.character.name, style = MaterialTheme.typography.titleSmall.merge(glassText),
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            GlassSurface(backdrop, appearance, shape = RoundedCornerShape(999.dp),
                modifier = Modifier.size(42.dp).bounceClick(enabled = !state.busy) {
                    onSetAutoSpeak(!state.data.ttsApi.autoSpeak)
                }) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SpeakerIcon(muted = !state.data.ttsApi.autoSpeak, color = foreground, size = 20.dp)
                }
            }
            GlassSurface(backdrop, appearance, shape = RoundedCornerShape(999.dp),
                modifier = Modifier.size(42.dp).bounceClick(enabled = !state.busy) { showTopMenu = !showTopMenu }) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MenuIcon(foreground, 20.dp) }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val bubbleMaxWidth = (maxWidth * 0.84f).coerceAtMost(560.dp)
            val renderMessage: @Composable (Message, Modifier) -> Unit = { message, itemModifier ->
                val user = message.role == Role.USER
                val isLatestAssistant = !user && message.id == latestAssistantId
                val isSpeakingThis = speech.speaking && speech.activeTag == message.id
                val isAnimating = !user && message.id == state.animatingMessageId
                val loadingHintHere = isLatestAssistant && state.loadingSuggestions && state.suggestionsMessageId == message.id
                val showHintsHere = isLatestAssistant && state.suggestionsMessageId == message.id && state.suggestions.isNotEmpty()
                val visibleCount = if (isAnimating) revealProgress[message.id] ?: 0 else message.content.length
                LaunchedEffect(message.id, isAnimating, message.content) {
                    if (!isAnimating) {
                        revealProgress.remove(message.id)
                    } else {
                        val full = message.content
                        val step = TypewriterPacing.stepSize(full)
                        var count = (revealProgress[message.id] ?: 0).coerceAtMost(full.length)
                        while (count < full.length) {
                            val next = (count + step).coerceAtMost(full.length)
                            revealProgress[message.id] = next
                            val lastIdx = next - 1
                            count = next
                            if (count < full.length) {
                                delay(TypewriterPacing.delayAfterCharMs(full, lastIdx))
                            }
                        }
                        onFinishAnimation(message.id)
                    }
                }
                LaunchedEffect(isAnimating, visibleCount / 28) {
                    if (isAnimating && visibleCount > 0 && totalListItems > 0) {
                        list.animateScrollToItem(totalListItems - 1)
                    }
                }
                val shownText = if (isAnimating && visibleCount < message.content.length) {
                    message.content.take(visibleCount)
                } else message.content
                Column(itemModifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
                    if (!user) {
                        GlassSurface(
                            backdrop, appearance, shape = RoundedCornerShape(999.dp),
                            tint = if (isSpeakingThis) Color(0xFFD9C7FF) else Color.White,
                            modifier = Modifier.padding(bottom = 4.dp).bounceClick {
                                if (isSpeakingThis) onStop() else onSpeak(message.content, message.id)
                            },
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                SoundWaveIcon(foreground, 14.dp, animated = isSpeakingThis)
                                Text(if (isSpeakingThis) "朗读中" else "朗读", style = MaterialTheme.typography.labelSmall.merge(glassText))
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.weight(1f, fill = false)) {
                            GlassSurface(
                                backdrop, appearance,
                                shape = RoundedCornerShape(18.dp),
                                tint = if (user) Color(0xFFB2E7FA) else Color.White,
                                modifier = Modifier.widthIn(max = if (isLatestAssistant) (bubbleMaxWidth - 44.dp).coerceAtLeast(180.dp) else bubbleMaxWidth).combinedClickable(
                                    onClick = {
                                        if (isAnimating) {
                                            revealProgress[message.id] = message.content.length
                                            onFinishAnimation(message.id)
                                        }
                                    },
                                    onLongClick = {
                                        if (!state.busy) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuMessageId = message.id
                                        }
                                    },
                                ),
                            ) {
                                Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(formatRoleplayText(shownText, foreground), style = MaterialTheme.typography.bodyLarge.merge(glassText))
                                }
                            }
                            DropdownMenu(
                                expanded = menuMessageId == message.id,
                                onDismissRequest = { menuMessageId = null },
                                containerColor = Color(0xEB23222B),
                            ) {
                                DropdownMenuItem(
                                    text = { Text("从此句继续对话（回溯）", color = Color.White) },
                                    onClick = { menuMessageId = null; onRewindTo(message.id) },
                                )
                                DropdownMenuItem(
                                    text = { Text("复制", color = Color.White) },
                                    onClick = { menuMessageId = null; clipboard.setText(AnnotatedString(message.content)) },
                                )
                                if (!user) {
                                    DropdownMenuItem(
                                        text = { Text("朗读此句", color = Color.White) },
                                        onClick = { menuMessageId = null; onSpeak(message.content, message.id) },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("删除此句对话", color = Color(0xFFFF8A9B)) },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuMessageId = null
                                        onDeleteMessage(message.id)
                                    },
                                )
                            }
                        }
                        if (isLatestAssistant) {
                            GlassSurface(
                                backdrop, appearance,
                                shape = RoundedCornerShape(999.dp),
                                tint = if (showHintsHere) Color(0xFFD6C4FF) else Color.White,
                                modifier = Modifier.size(36.dp).bounceClick(enabled = !state.sending) {
                                    onRequestSuggestions(message.id)
                                },
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (loadingHintHere) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = foreground)
                                    } else {
                                        LightbulbIcon(foreground, 18.dp)
                                    }
                                }
                            }
                        }
                    }
                    if (isLatestAssistant) {
                        AnimatedVisibility(
                            visible = showHintsHere,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                state.suggestions.forEachIndexed { idx, hint ->
                                    StaggeredEntrance(index = idx, stepDelayMs = 60L) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xCC1E1D28),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                            modifier = Modifier.fillMaxWidth().bounceClick(enabled = !state.busy && !pending, pressedScale = 0.97f) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onClearSuggestions()
                                                onSend(hint) {}
                                            },
                                        ) {
                                            Text(
                                                hint,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxSize()) {
                // Keep the control outside scrolling/animated messages, with a stable touch target.
                if (hasHiddenHistory) Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center) {
                    GlassSurface(backdrop, appearance, shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            .clickable(role = androidx.compose.ui.semantics.Role.Button) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showFullHistory = !showFullHistory
                            }) {
                        Text(if (showFullHistory) "⌄ 收起前文" else "⌃ 查看前文（$hiddenCount）",
                            style = MaterialTheme.typography.labelLarge.merge(glassText),
                            maxLines = 1, softWrap = false,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                    }
                }
            key(chat.id, showFullHistory) {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = list,
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = if (isFocusMode) Arrangement.spacedBy(12.dp, Alignment.Bottom) else Arrangement.spacedBy(12.dp),
            ) {
                if (showIntroCard) {
                    item(key = "intro-${chat.id}") {
                        GlassSurface(backdrop, appearance, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "简介  ${chat.character.description}",
                                style = MaterialTheme.typography.bodySmall.merge(glassText),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
                items(visibleMessages, key = { it.id }) { message ->
                    renderMessage(message, Modifier)
                }
                if (state.sending) {
                    item(key = "typing-indicator") {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GlassSurface(backdrop, appearance, shape = RoundedCornerShape(18.dp)) {
                                Row(
                                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TypingDotsIndicator(foreground)
                                }
                            }
                            GlassSurface(
                                backdrop, appearance, shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.bounceClick(onClick = onCancel),
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    StopIcon(foreground, 12.dp)
                                    Text("停止", style = MaterialTheme.typography.labelSmall.merge(glassText))
                                }
                            }
                        }
                    }
                }
            }
        }
            }
            }
        Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            backdrop.warning?.let {
                GlassSurface(backdrop, appearance, shape = RoundedCornerShape(14.dp)) {
                    Text(it, style = MaterialTheme.typography.labelSmall.merge(glassText), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
            if (showSpeechPill) {
                GlassSurface(backdrop, appearance, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(speechStatus, style = MaterialTheme.typography.labelSmall.merge(glassText), maxLines = 3,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Box(Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onStop), contentAlignment = Alignment.Center) {
                            StopIcon(foreground, 14.dp)
                        }
                    }
                }
            }
            if (pending && !state.busy) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassSurface(backdrop, appearance, shape = RoundedCornerShape(999.dp), modifier = Modifier.bounceClick(onClick = onRetry)) {
                        Text("重试回复", style = MaterialTheme.typography.labelMedium.merge(glassText),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                    GlassSurface(backdrop, appearance, shape = RoundedCornerShape(999.dp), modifier = Modifier.bounceClick(onClick = onDiscard)) {
                        Text("撤回未完成消息", style = MaterialTheme.typography.labelMedium.merge(glassText),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
            }
            GlassSurface(backdrop, appearance, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        if (draft.isEmpty()) {
                            Text("发消息…", style = MaterialTheme.typography.bodyLarge.merge(glassText).copy(color = foreground.copy(alpha = 0.6f)))
                        }
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            enabled = !state.busy && !pending,
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyLarge.merge(glassText),
                            cursorBrush = SolidColor(foreground),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(if (sendEnabled) YingYuAccentBrush else Brush.horizontalGradient(listOf(foreground.copy(alpha = 0.14f), foreground.copy(alpha = 0.14f))))
                            .bounceClick(enabled = sendEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSend(draft) { draft = "" }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        SendArrowIcon(if (sendEnabled) Color.White else foreground.copy(alpha = 0.45f), 20.dp)
                    }
                }
            }
        }
    }
    }
    if (showTopMenu) {
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { showTopMenu = false },
            ),
        )
    }
    AnimatedVisibility(
        visible = showTopMenu,
        enter = fadeIn(tween(180)) + scaleIn(tween(200, easing = FastOutSlowInEasing), initialScale = 0.88f, transformOrigin = TransformOrigin(1f, 0f)),
        exit = fadeOut(tween(140)) + scaleOut(tween(150), targetScale = 0.9f, transformOrigin = TransformOrigin(1f, 0f)),
        modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 58.dp, end = 12.dp),
    ) {
        val dangerColor = if (darkText) Color(0xFFB52845) else Color(0xFFFF8FA3)
        GlassSurface(
            backdrop,
            appearance,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.width(216.dp),
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                @Composable
                fun GlassMenuEntry(label: String, textColor: Color = foreground, onClick: () -> Unit) {
                    Box(
                        Modifier.fillMaxWidth().bounceClick(pressedScale = 0.96f, onClick = onClick)
                            .padding(horizontal = 18.dp, vertical = 13.dp),
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge.merge(glassText).copy(color = textColor),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                GlassMenuEntry("外观与字体") {
                    showTopMenu = false
                    appearanceDraft = state.data.appearance
                    customHexInput = state.data.appearance.textColorHex
                    showAppearance = true
                }
                GlassMenuEntry("角色声音与调音") {
                    showTopMenu = false; voiceDraft = chat.character.voice; showVoice = true
                }
                GlassMenuEntry("历史对话（${history.size}）") {
                    showTopMenu = false; showHistory = true
                }
                GlassMenuEntry("开启新对话") {
                    showTopMenu = false; onNew()
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    color = foreground.copy(alpha = 0.16f),
                )
                GlassMenuEntry("回溯人物（重回开场白）", textColor = dangerColor) {
                    showTopMenu = false; showResetConfirm = true
                }
            }
        }
    }
    AnimatedVisibility(
        visible = showAppearance,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("壁纸、玻璃与字体颜色（实时预览）", style = MaterialTheme.typography.titleMedium)
                Text("通透程度：${(100 - appearanceDraft.glassOpacity * 100).roundToInt()}%")
                Slider(1f - appearanceDraft.glassOpacity, { appearanceDraft = appearanceDraft.copy(glassOpacity = (1f - it).coerceIn(0f, 0.48f)) },
                    valueRange = 0.52f..1.0f, enabled = !state.busy)
                Text("背景模糊：${appearanceDraft.blurRadiusDp.roundToInt()}")
                Slider(appearanceDraft.blurRadiusDp, { appearanceDraft = appearanceDraft.copy(blurRadiusDp = it) },
                    valueRange = 0f..32f, enabled = !state.busy)
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("壁纸动态视差", style = MaterialTheme.typography.titleSmall)
                        Text("随手机轻微倾斜浮动，与毛玻璃形成景深感", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = appearanceDraft.parallaxEnabled,
                        onCheckedChange = { appearanceDraft = appearanceDraft.copy(parallaxEnabled = it) },
                        enabled = !state.busy,
                    )
                }
                Text("字体颜色", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TEXT_COLOR_PRESETS.forEach { (hex, label) ->
                        val rgb = ChatAppearance.parseHexRgb(hex) ?: 0xFFFFFF
                        val selected = appearanceDraft.resolvedTextRgb() == rgb
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(enabled = !state.busy) {
                                customHexInput = hex
                                appearanceDraft = appearanceDraft.copy(textColorHex = hex, darkText = appearanceDraft.copy(textColorHex = hex).isDarkTextColor())
                            }) {
                            Box(
                                Modifier.size(32.dp).clip(CircleShape)
                                    .background(Color(0xFF000000L or rgb.toLong()))
                                    .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape)
                            )
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                OutlinedTextField(
                    customHexInput,
                    { input ->
                        customHexInput = input
                        val normalized = ChatAppearance.normalizeHex(input)
                        if (normalized.isNotEmpty()) {
                            appearanceDraft = appearanceDraft.copy(textColorHex = normalized, darkText = appearanceDraft.copy(textColorHex = normalized).isDarkTextColor())
                        }
                    },
                    label = { Text("自定义字体色值（如 #111111）") },
                    singleLine = true,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }, enabled = !state.busy) { Text("更换本角色壁纸") }
                    TextButton(onClick = onResetWallpaper, enabled = !state.busy) { Text("恢复默认壁纸") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showAppearance = false }, enabled = !state.busy) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSaveAppearance(appearanceDraft.copy(textColorHex = customHexInput)) { showAppearance = false } },
                        enabled = !state.busy) { Text("保存外观") }
                }
            }
        }
    }
    }
}
