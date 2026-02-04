package com.vagueplayer.music.data.lyrics

import android.content.Context
import android.net.Uri
import android.util.Log
import com.vagueplayer.music.viewmodel.LyricLine
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.mp4.Mp4FieldKey
import org.jaudiotagger.tag.mp4.Mp4Tag
import java.io.File

/**
 * Minimal LRC Parser - Strictly follows LRC standard format
 */
object LrcParser {
    private const val TAG = "LrcParser"
    
    // Regex for LRC timestamp: [mm:ss.xx] or [mm:ss.xxx]
    private val timestampRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{2,3}))?]""")
    
    // Regex for metadata tags: [xx:...]
    private val metadataRegex = Regex("""^\[[a-zA-Z]+:.*]$""")
    
    private fun parseTimestamp(match: MatchResult): Long {
        val minutes = match.groupValues[1].toLongOrNull() ?: 0L
        val seconds = match.groupValues[2].toLongOrNull() ?: 0L
        val millisPart = match.groupValues[3]
        val millis = when (millisPart.length) {
            2 -> (millisPart.toLongOrNull() ?: 0L) * 10
            3 -> millisPart.toLongOrNull() ?: 0L
            else -> 0L
        }
        return (minutes * 60 + seconds) * 1000 + millis
    }
    
    private data class ParseEntry(
        val timeMs: Long,
        val text: String,
        val translation: String? = null,
        val syllables: List<Pair<Long, String>>? = null
    )

    fun parse(lrcContent: String): List<LyricLine> {
        val entries = mutableListOf<ParseEntry>()
        
        lrcContent.lines().forEach { line ->
            val trimmed = line.trim()
            
            // 1. Filter empty lines and comments
            if (trimmed.isEmpty()) return@forEach
            // Filter "QQ音乐..." copyright lines or "//" comments
            if (trimmed.startsWith("//") || trimmed.contains("QQ音乐")) return@forEach
            
            // Skip metadata lines like [ar:Artist], [ti:Title]
            if (metadataRegex.matches(trimmed)) return@forEach
            
            // Find all timestamps in this line
            val timestamps = timestampRegex.findAll(trimmed).toList()
            if (timestamps.isEmpty()) return@forEach
            
            // Extract text by removing ALL timestamps
            val rawText = timestampRegex.replace(trimmed, "").trim()
            
            // Skip if text is empty or just special chars (like // after timestamp override)
            if (rawText.isEmpty() || rawText.startsWith("//")) return@forEach
            
            // Detect inline bilingual lyrics (OLD logic kept for backward compat)
            val (mainText, inlineTrans) = parseInlineTranslation(rawText)
            
            // Check for Karaoke style: timestamps interleaved with text
            // e.g. [00:01.00]Word[00:01.50]Another
            val isKaraoke = timestamps.count() > 1 && timestamps.zipWithNext().any { (current, next) ->
                // Check content between current match end and next match start
                val gap = trimmed.substring(current.range.last + 1, next.range.first)
                gap.isNotBlank()
            }
            
            if (isKaraoke) {
                // Karaoke: Extract syllables
                val syllables = mutableListOf<Pair<Long, String>>()
                val sortedTimestamps = timestamps.sortedBy { it.range.first }.toList()
                
                var combinedText = ""
                
                sortedTimestamps.forEachIndexed { index, matchResult ->
                    val startTime = parseTimestamp(matchResult)
                    val endTimeIndex = if (index < sortedTimestamps.size - 1) sortedTimestamps[index + 1].range.first else trimmed.length
                    
                    // Extract text until next timestamp or end of line
                    val syllableText = trimmed.substring(matchResult.range.last + 1, endTimeIndex)
                    if (syllableText.isNotEmpty()) {
                         syllables.add(startTime to syllableText)
                         combinedText += syllableText
                    }
                }
                
                // Use the first timestamp for the line's start time
                val lineStartTime = syllables.firstOrNull()?.first ?: parseTimestamp(timestamps.first())
                
                // Add single entry with syllable data
                entries.add(ParseEntry(lineStartTime, combinedText, inlineTrans, syllables))
            } else {
                // Standard: Add entry for EACH timestamp
                timestamps.forEach { match ->
                    val timeMs = parseTimestamp(match)
                    entries.add(ParseEntry(timeMs, mainText, inlineTrans))
                }
            }
        }
        
        // 2. Merge lines with SAME timestamp (e.g. [00:01]Korean \n [00:01]Chinese)
        return entries.groupBy { it.timeMs } // Group by Time
            .map { (time, group) ->
                // group: List<ParseEntry>
                // Assume the FIRST line encountering this timestamp is the Original Text
                val primaryLine = group.first()
                
                // Determine translation:
                // Priority 1: A distinct second line sharing the same timestamp (Multi-line lrc)
                // Priority 2: Inline translation from the first line
                val otherLines = group.drop(1).map { it.text }.filter { it != primaryLine.text }
                
                val finalTranslation = if (otherLines.isNotEmpty()) {
                    otherLines.joinToString("\n")
                } else {
                    primaryLine.translation // Fallback to inline
                }
                
                LyricLine(time, primaryLine.text, finalTranslation, primaryLine.syllables)
            }
            .sortedBy { it.timeMs }
    }

    private fun parseInlineTranslation(text: String): Pair<String, String?> {
        if (text.contains(" / ")) {
            val parts = text.split(" / ", limit = 2)
            return parts[0].trim() to parts.getOrNull(1)?.trim()
        } else if (text.contains("/") && !text.contains("://")) {
            // Avoid splitting URLs
            val parts = text.split("/", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                return parts[0].trim() to parts[1].trim()
            }
        }
        return text to null
    }
    
    fun loadLyricsForSong(context: Context, audioUri: Uri): List<LyricLine> {
        Log.d(TAG, "loadLyricsForSong: $audioUri")
        try {
            // 1. Try embedded lyrics
            val embeddedLyrics = getEmbeddedLyrics(context, audioUri)
            if (embeddedLyrics.isNotEmpty()) {
                Log.d(TAG, "Found embedded lyrics (${embeddedLyrics.length} chars), parsing...")
                return parse(embeddedLyrics)
            }
            
            // 2. Try external .lrc file
            val audioPath = getFilePathFromUri(context, audioUri) ?: return emptyList()
            val audioFile = File(audioPath)
            val lrcFile = File(audioFile.parentFile, audioFile.nameWithoutExtension + ".lrc")
            
            if (lrcFile.exists()) {
                Log.d(TAG, "Found LRC file: ${lrcFile.absolutePath}")
                val bytes = lrcFile.readBytes()
                val content = try {
                    String(bytes, Charsets.UTF_8).takeIf { !it.contains("\uFFFD") }
                } catch (e: Exception) {
                    null
                } ?: String(bytes, java.nio.charset.Charset.forName("GBK"))
                
                return parse(content)
            } else {
                Log.d(TAG, "No LRC file found for: ${audioFile.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading lyrics", e)
        }
        
        return emptyList()
    }
    
    private fun getEmbeddedLyrics(context: Context, audioUri: Uri): String {
        Log.d(TAG, "=== getEmbeddedLyrics START ===")
        try {
            val filePath = getFilePathFromUri(context, audioUri)
            if (filePath == null) {
                Log.w(TAG, "Failed to get file path from URI")
                return ""
            }
            
            val file = File(filePath)
            if (!file.exists()) {
                Log.w(TAG, "File does not exist: $filePath")
                return ""
            }
            
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag
            if (tag == null) {
                Log.w(TAG, "No tag found")
                return ""
            }
            Log.d(TAG, "Tag type: ${tag.javaClass.simpleName}")
            
            // Try standard LYRICS field
            val lyrics = tag.getFirst(FieldKey.LYRICS)
            Log.d(TAG, "Standard lyrics field length: ${lyrics.length}")
            
            if (lyrics.isNotEmpty()) {
                // Check if it looks like XML/HTML junk or real lyrics
                if (lyrics.trim().startsWith("<") && !lyrics.contains("[")) {
                     Log.w(TAG, "Lyrics content looks like XML/HTML, ignoring")
                } else {
                    return lyrics
                }
            }
            
            // For MP4/M4A files
            if (filePath.endsWith(".m4a", ignoreCase = true) || filePath.endsWith(".mp4", ignoreCase = true)) {
                try {
                    val mp4Tag = tag as? Mp4Tag
                    val lyricsField = mp4Tag?.getFirst(Mp4FieldKey.LYRICS)
                    Log.d(TAG, "MP4 lyrics field length: ${lyricsField?.length}")
                    
                    if (lyricsField?.isNotEmpty() == true) {
                        return lyricsField
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading MP4 lyrics", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getEmbeddedLyrics", e)
        }
        return ""
    }
    
    private fun getFilePathFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        
        if (uri.scheme == "content") {
            try {
                val projection = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                        return cursor.getString(columnIndex)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting file path from URI", e)
            }
        }
        return null
    }
}
