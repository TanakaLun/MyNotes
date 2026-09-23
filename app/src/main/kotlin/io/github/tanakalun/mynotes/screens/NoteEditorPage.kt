package io.github.tanakalun.mynotes.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.tanakalun.mynotes.R
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import io.github.tanakalun.mynotes.SettingsStore
import io.github.tanakalun.mynotes.data.ContentSegment
import io.github.tanakalun.mynotes.data.Note
import io.github.tanakalun.mynotes.data.NoteRepository
import io.github.tanakalun.mynotes.data.parseSegmentsFromMarkdown
import io.github.tanakalun.mynotes.data.serializeSegmentsToMarkdown
import io.github.tanakalun.mynotes.navigation.Navigator
import io.github.tanakalun.mynotes.navigation.Route
import io.github.tanakalun.mynotes.ui.highlightCode
import io.github.tanakalun.mynotes.utils.BackNavigationIcon
import io.github.tanakalun.mynotes.utils.ImageUtils
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Unlock
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.LocalContentColor

@OptIn(ExperimentalRichTextApi::class)
@Composable
fun NoteEditorPage(
    noteId: String,
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    navigator: Navigator,
) {
    val context = LocalContext.current
    val existing = remember(noteId) {
        if (noteId.isNotBlank()) NoteRepository.getById(noteId) else null
    }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var colorIndex by remember { mutableIntStateOf(existing?.colorIndex ?: 0) }
    var readOnly by remember { mutableStateOf(false) }

    val initialSegments = remember(existing) {
        val parsed = existing?.let {
            parseSegmentsFromMarkdown(it.content)
        } ?: listOf(ContentSegment.Text())
        mutableStateListOf<ContentSegment>().apply { addAll(parsed) }
    }
    val segments = initialSegments

    val richTextStates = remember { mutableMapOf<String, com.mohamedrejeb.richeditor.model.RichTextState>() }

    LaunchedEffect(existing) {
        if (existing != null && existing.content.isNotBlank()) {
            val parsed = parseSegmentsFromMarkdown(existing.content)
            segments.clear()
            segments.addAll(parsed)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            richTextStates.clear()
        }
    }

    var focusedSegmentId by remember { mutableStateOf<String?>(null) }

    fun findSegmentIndex(segmentId: String): Int {
        return segments.indexOfFirst { it.id == segmentId }
    }

    val isNew = existing == null

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        val insertIndex = focusedSegmentId?.let { findSegmentIndex(it) }?.plus(1) ?: segments.size
        var offset = 0
        uris.forEach { uri ->
            val path = ImageUtils.copyImageToInternal(context, uri)
            if (path != null) {
                val (w, h) = ImageUtils.getImageDimensions(path)
                segments.add(insertIndex + offset, ContentSegment.Image(path = path, width = w, height = h))
                offset++
            }
        }
        segments.add(insertIndex + offset, ContentSegment.Text())
    }

    fun saveAndBack() {
        val allMarkdown = buildString {
            segments.forEach { segment ->
                when (segment) {
                    is ContentSegment.Text -> {
                        val state = richTextStates[segment.id]
                        append(state?.toMarkdown() ?: segment.markdown)
                    }
                    is ContentSegment.Image -> {
                        append("![image](${segment.path})")
                    }
                    is ContentSegment.Code -> {
                        if (segment.language.isNotBlank()) {
                            append("```${segment.language}\n${segment.code}\n```")
                        } else {
                            append("```\n${segment.code}\n```")
                        }
                    }
                }
            }
        }

        val imagePaths = segments.filterIsInstance<ContentSegment.Image>().map { it.path }

        if (title.isNotBlank() || allMarkdown.isNotBlank()) {
            val note = existing?.copy(
                title = title,
                content = allMarkdown,
                colorIndex = colorIndex,
                images = imagePaths,
            ) ?: Note.createNote(
                title = title,
                content = allMarkdown,
                colorIndex = colorIndex,
            )
            NoteRepository.upsert(note)
        }
        onBack()
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(
                    if (isNew) R.string.new_note else R.string.edit_note
                ),
                color = MiuixTheme.colorScheme.surface,
                navigationIcon = {
                    BackNavigationIcon(onClick = ::saveAndBack)
                },
                actions = {
                    IconButton(onClick = { readOnly = !readOnly }) {
                        Icon(
                            imageVector = if (readOnly) MiuixIcons.Unlock else MiuixIcons.Lock,
                            contentDescription = stringResource(R.string.read_only),
                        )
                    }
                    IconButton(onClick = ::saveAndBack) {
                        Icon(
                            imageVector = MiuixIcons.Ok,
                            contentDescription = stringResource(R.string.save),
                        )
                    }
                },
            )
        },
    ) { scaffoldPadding ->
        val topPadding = scaffoldPadding.calculateTopPadding()
        val bottomPadding = innerPadding.calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = topPadding,
                    bottom = bottomPadding,
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        10.dp,
                        Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val defaultSwatch = MiuixTheme.colorScheme.surfaceContainer
                    val outlineColor = MiuixTheme.colorScheme.outline
                    val swatchColors = remember(defaultSwatch) {
                        listOf(defaultSwatch) + Note.COLORS.map { Color(it) }
                    }
                    swatchColors.forEachIndexed { index, color ->
                        val selected = colorIndex == index
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selected) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            shape = CircleShape,
                                        )
                                    } else {
                                        Modifier.border(
                                            width = 1.dp,
                                            color = outlineColor.copy(alpha = 0.2f),
                                            shape = CircleShape,
                                        )
                                    },
                                )
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                    onClick = { if (!readOnly) colorIndex = index },
                                ),
                        )
                    }
                }

                TextField(
                    value = title,
                    onValueChange = { if (!readOnly) title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textStyle = MiuixTheme.textStyles.title1.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    ),
                    label = stringResource(R.string.title_field),
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    readOnly = readOnly,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .squircleBackground(
                        color = MiuixTheme.colorScheme.secondaryContainer,
                        cornerRadius = 16.dp,
                    )
                    .padding(16.dp),
            ) {
                val contentColor = LocalContentColor.current

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    segments.forEachIndexed { index, segment ->
                        when (segment) {
                            is ContentSegment.Text -> {
                                val richState = rememberRichTextState()
                                LaunchedEffect(segment.id, segment.markdown) {
                                    val currentMd = richState.toMarkdown()
                                    if (currentMd != segment.markdown) {
                                        richState.setMarkdown(segment.markdown)
                                    }
                                }
                                LaunchedEffect(segment.id) {
                                    richTextStates[segment.id] = richState
                                }
                                BasicRichTextEditor(
                                    state = richState,
                                    readOnly = readOnly,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                focusedSegmentId = segment.id
                                            }
                                        },
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        color = contentColor,
                                    ),
                                    cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                                    decorationBox = @Composable { innerTextField ->
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.TopStart,
                                        ) {
                                            if (richState.annotatedString.isEmpty() && segments.size == 1) {
                                                Text(
                                                    text = stringResource(R.string.start_writing),
                                                    style = MiuixTheme.textStyles.body1.copy(
                                                        fontSize = 16.sp,
                                                        lineHeight = 24.sp,
                                                        color = contentColor.copy(alpha = 0.7f),
                                                    ),
                                                )
                                            }
                                            innerTextField()
                                        }
                                    },
                                )
                            }
                            is ContentSegment.Code -> {
                                val codeBlockWrap = SettingsStore.codeBlockWrap
                                val highlighted = remember(segment.code, segment.language) {
                                    highlightCode(segment.code, segment.language)
                                }
                                val codeScrollState = rememberScrollState()

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MiuixTheme.colorScheme.surfaceContainer)
                                        .padding(12.dp),
                                ) {
                                    if (segment.language.isNotBlank()) {
                                        Text(
                                            text = segment.language,
                                            style = MiuixTheme.textStyles.footnote1.copy(
                                                fontSize = 11.sp,
                                            ),
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            modifier = Modifier.padding(bottom = 4.dp),
                                        )
                                    }
                                    SelectionContainer {
                                        if (codeBlockWrap) {
                                            Text(
                                                text = highlighted,
                                                style = TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp,
                                                    color = contentColor,
                                                ),
                                            )
                                        } else {
                                            Text(
                                                text = highlighted,
                                                style = TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp,
                                                    color = contentColor,
                                                ),
                                                modifier = Modifier.horizontalScroll(codeScrollState),
                                                maxLines = Int.MAX_VALUE,
                                            )
                                        }
                                    }
                                }
                            }
                            is ContentSegment.Image -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(segment.path)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = stringResource(R.string.note_image),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(
                                                if (segment.width > 0 && segment.height > 0) {
                                                    segment.width.toFloat() / segment.height.toFloat()
                                                } else {
                                                    16f / 9f
                                                }
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                navigator.push(Route.ImageViewer(segment.path))
                                            },
                                        contentScale = ContentScale.Crop,
                                    )
                                    IconButton(
                                        onClick = {
                                            if (readOnly) return@IconButton
                                            val textBefore = segments.getOrNull(index - 1)
                                            val textAfter = segments.getOrNull(index + 1)
                                            segments.removeAt(index)
                                            if (textBefore is ContentSegment.Text && textAfter is ContentSegment.Text) {
                                                val merged = textBefore.copy(
                                                    markdown = textBefore.markdown + textAfter.markdown
                                                )
                                                segments[index - 1] = merged
                                                segments.removeAt(index)
                                            }
                                        },
                                        enabled = !readOnly,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                    ) {
                                        Icon(
                                            imageVector = MiuixIcons.Delete,
                                            contentDescription = stringResource(R.string.delete_image),
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!readOnly) {
                FloatingToolbar(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp),
                    color = MiuixTheme.colorScheme.surfaceContainer,
                    cornerRadius = 20.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FormatButton(label = "B", description = stringResource(R.string.toolbar_bold)) {
                            val segId = focusedSegmentId
                            if (segId != null) {
                                richTextStates[segId]?.toggleSpanStyle(
                                    SpanStyle(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        FormatButton(label = "I", description = stringResource(R.string.toolbar_italic)) {
                            val segId = focusedSegmentId
                            if (segId != null) {
                                richTextStates[segId]?.toggleSpanStyle(
                                    SpanStyle(fontStyle = FontStyle.Italic)
                                )
                            }
                        }
                        FormatButton(label = "U", description = stringResource(R.string.toolbar_underline)) {
                            val segId = focusedSegmentId
                            if (segId != null) {
                                richTextStates[segId]?.toggleSpanStyle(
                                    SpanStyle(textDecoration = TextDecoration.Underline)
                                )
                            }
                        }
                        FormatButton(label = "H1", description = stringResource(R.string.toolbar_heading1)) {
                            val segId = focusedSegmentId
                            if (segId != null) {
                                richTextStates[segId]?.setHeadingStyle(HeadingStyle.H1)
                            }
                        }
                        FormatButton(label = "H2", description = stringResource(R.string.toolbar_heading2)) {
                            val segId = focusedSegmentId
                            if (segId != null) {
                                richTextStates[segId]?.setHeadingStyle(HeadingStyle.H2)
                            }
                        }
                        FormatButton(label = "•", description = stringResource(R.string.toolbar_unordered_list)) {
                            val segId = focusedSegmentId
                            if (segId != null) {
                                richTextStates[segId]?.toggleUnorderedList()
                            }
                        }
                        FormatButton(label = "1.", description = stringResource(R.string.toolbar_ordered_list)) {
                            val segId = focusedSegmentId
                            if (segId != null) {
                                richTextStates[segId]?.toggleOrderedList()
                            }
                        }
                        FormatIconButton(
                            description = stringResource(R.string.add_image),
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
            )
        }
    }
}

@Composable
private fun FormatButton(
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FormatIconButton(
    description: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Icon(
            imageVector = MiuixIcons.AddCircle,
            contentDescription = description,
            tint = MiuixTheme.colorScheme.onSurface,
        )
    }
}
