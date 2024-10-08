package io.mocha.music.lyrics.parser

import io.mocha.music.lyrics.model.SyncedLyrics
import io.mocha.music.lyrics.model.synced.UncheckedSyncedLine
import io.mocha.music.lyrics.utils.parseAsTime

object LrcParser : ILyricsParser {
    private val parser = Regex("\\[(\\d{1,2}:\\d{1,2}\\.\\d{2,3})](.*)")

    override fun parse(lines: List<String>): SyncedLyrics {
        val lyricsLines = AttributesHelper.removeAttributes(lines)
        val data = lyricsLines
            .flatMap { line -> parseLine(line) }
            .rearrangeTime()
            .combineRawWithTranslation()
            .map { it.toSyncedLine() }
            .filter { it.content.isNotBlank() }
            .sortedBy { it.start }
        return SyncedLyrics(lines = data)
    }

    private fun parseLine(content: String): List<UncheckedSyncedLine> {
        return parser.findAll(content).map { matchResult ->
            val (time, lyric) = matchResult.destructured
            UncheckedSyncedLine(
                start = time.parseAsTime(),
                end = 0,
                content = lyric.trim(),
                translation = null
            )
        }.toList()
    }

    private fun List<UncheckedSyncedLine>.combineRawWithTranslation(): List<UncheckedSyncedLine> {
        val list = mutableListOf<UncheckedSyncedLine>()
        var i = 0
        while (i < this.size) {
            val line = this[i]
            val nextLine = this.getOrNull(i + 1)
            if (nextLine != null && line.start == nextLine.start) {
                list.add(line.copy(translation = nextLine.content))
                i += 2  // 跳过下一行，因为它是翻译
            } else {
                list.add(line)
                i++
            }
        }
        return list
    }

    private fun List<UncheckedSyncedLine>.rearrangeTime(): List<UncheckedSyncedLine> {
        return this.mapIndexed { index, line ->
            val end = this.getOrNull(index + 1)?.start ?: Int.MAX_VALUE
            line.copy(end = end)
        }
    }
}



