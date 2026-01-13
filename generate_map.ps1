$url = "https://raw.githubusercontent.com/belerweb/pinyin4j/master/src/main/resources/pinyindb/unicode_to_hanyu_pinyin.txt"
$outputFile = "app/src/main/java/com/vagueplayer/music/utils/PinyinMapData.kt"

Write-Host "Downloading Pinyin DB..."
$tempFile = "$env:TEMP\pinyin.txt"
# Only download if not exists to save time/bandwidth during dev iter
if (-not (Test-Path $tempFile)) {
    Invoke-WebRequest -Uri $url -OutFile $tempFile
}

Write-Host "Parsing..."
$content = Get-Content $tempFile

# Hashtable: Syllable -> StringBuilder (e.g., "AI" -> "\u7231...")
$syllableBuckets = @{}

$count = 0

foreach ($line in $content) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }

    if ($line -match "^([0-9A-F]{4})\s+(.*)") {
        $hex = $matches[1]
        $rawPinyin = $matches[2]
        
        # Clean: (ling2) -> LING
        $cleaned = $rawPinyin -replace "[\(\)\s0-9]", "" # Remove parens, space, numbers
        $cleaned = $cleaned.ToUpper()
        
        # Handle multiple "YI,YI" -> "YI"
        if ($cleaned -contains ",") {
            $cleaned = $cleaned.Split(",")[0]
        }
        
        if ($cleaned.Length -gt 0) {
            # Validate Key (A-Z only)
            if ($cleaned -match "^[A-Z]+$") {
                if (-not $syllableBuckets.ContainsKey($cleaned)) {
                    $syllableBuckets[$cleaned] = [System.Text.StringBuilder]::new()
                }
                $syllableBuckets[$cleaned].Append("\u$hex") | Out-Null
                $count++
            }
        }
    }
}

Write-Host "Processed $count characters into $($syllableBuckets.Count) syllables."

# Generate Kotlin Code
$kotlinCode = @"
package com.vagueplayer.music.utils

/**
 * Auto-generated Pinyin Index Data.
 * Contains Chinese characters grouped by Full Pinyin Syllable for accurate sorting.
 */
object PinyinMapData {
"@
$kotlinCode += "`r`n"

# 1. Output Syllable Constants (S_AI, S_AN...)
$sortedSyllables = $syllableBuckets.Keys | Sort-Object

foreach ($key in $sortedSyllables) {
    $str = $syllableBuckets[$key].ToString()
    # Split checks for safety (though unlikely to exceed 65k for single syllable)
    if ($str.Length -gt 32000) {
        $part1 = $str.Substring(0, 32000)
        $part2 = $str.Substring(32000)
        $kotlinCode += "    private const val S_${key}_1 = `"$part1`"`r`n"
        $kotlinCode += "    private const val S_${key}_2 = `"$part2`"`r`n"
        $kotlinCode += "    private const val S_${key} = S_${key}_1 + S_${key}_2`r`n"
    }
    else {
        $kotlinCode += "    private const val S_${key} = `"$str`"`r`n"
    }
}

$kotlinCode += "`r`n    // --- Initial Groups (Union of Syllables) ---`r`n"

# 2. Output Initial Group Constants (Union)
# CHARS_A = S_A + S_AI + S_AN ...
65..90 | ForEach-Object {
    $char = [char]$_
    $syllablesInGroup = $sortedSyllables | Where-Object { $_.StartsWith($char) }
    
    if ($syllablesInGroup) {
        $parts = $syllablesInGroup | ForEach-Object { "S_$_" }
        $unionExpr = $parts -join " + "
        $kotlinCode += "    const val CHARS_$char = $unionExpr`r`n"
    }
    else {
        $kotlinCode += "    const val CHARS_$char = `"`"`r`n"
    }
}

$kotlinCode += @"

    /**
     * Gets the Pinyin Initial (A-Z) for a character.
     * Fast lookup using grouped constants.
     */
    fun getInitial(c: Char): Char {
"@
$kotlinCode += "`r`n"

65..90 | ForEach-Object {
    $char = [char]$_
    $kotlinCode += "        if (CHARS_$char.contains(c)) return '$char'`r`n"
}

$kotlinCode += @"
        return '#'
    }

    /**
     * Gets the Full Pinyin Syllable for a character.
     * Returns null if not a mapped Chinese character.
     */
    fun getPinyin(c: Char): String? {
        val initial = getInitial(c)
        if (initial == '#') return null
        
        // Two-level lookup: Dispatch by Initial, then check Syllables
        when (initial) {
"@
$kotlinCode += "`r`n"

65..90 | ForEach-Object {
    $char = [char]$_
    $syllables = $sortedSyllables | Where-Object { $_.StartsWith($char) }
    
    if ($syllables) {
        $kotlinCode += "            '$char' -> {`r`n"
        # Sort by usage? Or length? No, just iterate.
        foreach ($syl in $syllables) {
            $kotlinCode += "                if (S_$syl.contains(c)) return `"$syl`"`r`n"
        }
        $kotlinCode += "            }`r`n"
    }
}

$kotlinCode += @"
        }
        return null
    }
}
"@

Set-Content -Path $outputFile -Value $kotlinCode -Encoding UTF8
Write-Host "Generated $outputFile"
