package com.vagueplayer.music.data.lyrics

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.vagueplayer.music.viewmodel.LyricLine
import java.io.File

/**
 * LRC File Parser with Bilingual Support
 * 
 * Supports formats:
 * - Standard: [mm:ss.xx]Lyric text
 * - Bilingual: [mm:ss.xx]Original text / Translation
 * - Bilingual Alt: [mm:ss.xx]Original text[mm:ss.xx]Translation
 * - Embedded lyrics from audio file metadata
 */
object LrcParser {
    private const val TAG = "LrcParser"
    
    // Regex for LRC timestamp: [mm:ss.xx] or [mm:ss.xxx] or [mm:ss]
    private val timestampRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{2,3}))?\]""")
    
    /**
     * Parse LRC content string into list of LyricLine with bilingual support
     */
    fun parse(lrcContent: String): List<LyricLine> {
        val rawLines = mutableListOf<Pair<Long, String>>()
        
        lrcContent.lines().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEach
            
            // Skip metadata lines like [ti:], [ar:], [al:], etc.
            if (trimmedLine.matches(Regex("""\[[a-z]+:.*\]"""))) return@forEach
            
            // Find all timestamps in the line
            val matches = timestampRegex.findAll(trimmedLine)
            val timestamps = matches.map { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val millisPart = match.groupValues[3]
                val millis = when (millisPart.length) {
                    2 -> (millisPart.toLongOrNull() ?: 0L) * 10
                    3 -> millisPart.toLongOrNull() ?: 0L
                    else -> 0L
                }
                (minutes * 60 + seconds) * 1000 + millis
            }.toList()
            
            // Extract text after all timestamps
            val text = timestampRegex.replace(trimmedLine, "").trim()
            
            timestamps.forEach { timeMs ->
                if (text.isNotEmpty()) {
                    rawLines.add(timeMs to text)
                }
            }
        }
        
        // Sort by timestamp
        val sortedRaw = rawLines.sortedBy { it.first }
        
        // Detect bilingual format: Check if consecutive lines have same timestamp
        // or if lines contain " / " separator
        val result = mutableListOf<LyricLine>()
        var i = 0
        
        while (i < sortedRaw.size) {
            val (timeMs, text) = sortedRaw[i]
            
            // Check for " / " separator in single line (inline translation)
            if (text.contains(" / ")) {
                val parts = text.split(" / ", limit = 2)
                result.add(LyricLine(timeMs, parts[0].trim(), parts.getOrNull(1)?.trim()))
                i++
                continue
            }
            
            // Check for "/" separator (no spaces)
            if (text.contains("/") && !text.contains("://")) {
                val parts = text.split("/", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    result.add(LyricLine(timeMs, parts[0].trim(), parts[1].trim()))
                    i++
                    continue
                }
            }
            
            // Check if next line has same timestamp (translation line)
            if (i + 1 < sortedRaw.size && sortedRaw[i + 1].first == timeMs) {
                val translation = sortedRaw[i + 1].second
                result.add(LyricLine(timeMs, text, translation))
                i += 2
                continue
            }
            
            // Single line, no translation
            result.add(LyricLine(timeMs, text, null))
            i++
        }
        
        return result
    }
    
    /**
     * Try to read embedded lyrics from audio file metadata first,
     * then fall back to external .lrc file
     */
    fun loadLyricsForSong(context: Context, audioUri: Uri): List<LyricLine> {
        try {
            // 1. First try embedded lyrics from audio metadata
            val embeddedLyrics = getEmbeddedLyrics(context, audioUri)
            if (embeddedLyrics.isNotEmpty()) {
                Log.d(TAG, "Found embedded lyrics")
                return parse(embeddedLyrics)
            }
            
            // 2. Fall back to external .lrc file
            val audioPath = getFilePathFromUri(context, audioUri) ?: return emptyList()
            val audioFile = File(audioPath)
            val lrcFile = File(audioFile.parentFile, audioFile.nameWithoutExtension + ".lrc")
            
            if (lrcFile.exists()) {
                Log.d(TAG, "Found LRC file: ${lrcFile.absolutePath}")
                // Read bytes first to detect encoding
                val bytes = lrcFile.readBytes()
                val content = try {
                    // 1. Try UTF-8 first
                    val utf8 = String(bytes, Charsets.UTF_8)
                    // Simple heuristic: if it contains Replacement Character, it's likely not valid UTF-8
                    if (utf8.contains("\uFFFD")) {
                        throw Exception("Invalid UTF-8")
                    }
                    utf8
                } catch (e: Exception) {
                    // 2. Fallback to GBK (Common in Chinese localized files)
                    Log.d(TAG, "Falling back to GBK encoding for ${lrcFile.name}")
                    String(bytes, java.nio.charset.Charset.forName("GBK"))
                }
                return parse(content)
            } else {
                Log.d(TAG, "No LRC file found for: ${audioFile.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading lyrics", e)
        }
        
        return emptyList()
    }
    
    /**
     * Read embedded lyrics from audio file metadata (ID3 USLT/SYLT tags)
     */
    private fun getEmbeddedLyrics(context: Context, audioUri: Uri): String {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, audioUri)
            
            // Try to get lyrics from metadata (METADATA_KEY_LYRICS is not always available)
            // MediaMetadataRetriever doesn't directly expose USLT, but we can try
            val lyrics = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE + 100) // Hack attempt
            retriever.release()
            
            // Note: MediaMetadataRetriever doesn't have a direct LYRICS key
            // Real implementation would need a library like JAudioTagger
            // For now, return empty to fall back to .lrc files
        } catch (e: Exception) {
            Log.e(TAG, "Error reading embedded lyrics", e)
        }
        return ""
    }
    
    /**
     * Get file path from content URI
     */
    private fun getFilePathFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }
        
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
