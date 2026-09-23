package io.github.tanakalun.mynotes.data

import java.util.UUID

sealed class ContentSegment {
    abstract val id: String

    data class Text(
        override val id: String = UUID.randomUUID().toString(),
        val markdown: String = "",
    ) : ContentSegment()

    data class Image(
        override val id: String = UUID.randomUUID().toString(),
        val path: String,
        val width: Int = 0,
        val height: Int = 0,
    ) : ContentSegment()

    data class Code(
        override val id: String = UUID.randomUUID().toString(),
        val language: String = "",
        val code: String = "",
    ) : ContentSegment()
}

private val IMAGE_MARKER_REGEX = Regex("""!\[.*?\]\((.*?)\)""")
private val FENCED_CODE_REGEX = Regex("""```(\w*)\n([\s\S]*?)```""")

fun parseSegmentsFromMarkdown(markdown: String): List<ContentSegment> {
    if (markdown.isBlank()) return listOf(ContentSegment.Text())

    val codeBlockRanges = FENCED_CODE_REGEX.findAll(markdown).map {
        it.range.first to it.range.last + 1
    }.toMutableList()

    val imageRanges = mutableListOf<Pair<Int, Int>>()
    IMAGE_MARKER_REGEX.findAll(markdown).forEach { match ->
        val path = match.groupValues[1]
        if (path.startsWith("/") || path.startsWith("file:")) {
            imageRanges.add(match.range.first to match.range.last + 1)
        }
    }

    val allRanges = (codeBlockRanges + imageRanges).sortedBy { it.first }

    val segments = mutableListOf<ContentSegment>()
    var lastEnd = 0

    FENCED_CODE_REGEX.findAll(markdown).forEach { match ->
        val codeStart = match.range.first
        val codeEnd = match.range.last + 1

        if (codeStart > lastEnd) {
            val before = markdown.substring(lastEnd, codeStart)
            if (before.isNotBlank()) {
                segments.add(ContentSegment.Text(markdown = before))
            }
        }

        val language = match.groupValues[1]
        val code = match.groupValues[2].removeSuffix("\n")
        segments.add(ContentSegment.Code(language = language, code = code))

        lastEnd = codeEnd
    }

    IMAGE_MARKER_REGEX.findAll(markdown).forEach { match ->
        val path = match.groupValues[1]
        if (path.startsWith("/") || path.startsWith("file:")) {
            val imgStart = match.range.first
            val imgEnd = match.range.last + 1

            if (imgStart > lastEnd) {
                val before = markdown.substring(lastEnd, imgStart)
                if (before.isNotBlank()) {
                    segments.add(ContentSegment.Text(markdown = before))
                }
            }

            segments.add(ContentSegment.Image(path = path))
            lastEnd = imgEnd
        }
    }

    val remaining = markdown.substring(lastEnd)
    if (remaining.isNotBlank()) {
        segments.add(ContentSegment.Text(markdown = remaining))
    }

    if (segments.isEmpty()) {
        segments.add(ContentSegment.Text())
    }

    return segments
}

fun serializeSegmentsToMarkdown(segments: List<ContentSegment>): String {
    return segments.joinToString("") { segment ->
        when (segment) {
            is ContentSegment.Text -> segment.markdown
            is ContentSegment.Image -> "![image](${segment.path})"
            is ContentSegment.Code -> {
                if (segment.language.isNotBlank()) {
                    "```${segment.language}\n${segment.code}\n```"
                } else {
                    "```\n${segment.code}\n```"
                }
            }
        }
    }
}
