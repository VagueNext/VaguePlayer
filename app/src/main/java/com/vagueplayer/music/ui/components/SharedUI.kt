package com.vagueplayer.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

// Moved from BottomNavBar.kt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector


/**
 * Standardized Header for all main screens (Library, Playlist, Profile, etc.)
 * Enforces consistent Top Padding (80dp) and Typography (34sp Bold).
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp) // [FIX] Add standard horizontal padding
            .padding(top = 80.dp, bottom = 16.dp), // Standardized Position
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Title
        Text(
            text = title,
            fontSize = 32.sp, // [FIX] Match Library (32sp)
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        // Optional Action Button (e.g. "Create", "Settings")
        if (action != null) {
            action()
        }
    }
}

@Composable
fun RoundedRepeatIcon(
    count: String? = null, // Null = Repeat All/Off, "1" = One, "N" = Placeholder
    color: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(modifier = modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.5.dp.toPx()
            val w = size.width
            val h = size.height
            val cornerRadius = w * 0.35f 
            
            // Defines the gap size at the top
            val gapSize = 6.dp.toPx() 

            // 1. Draw Loop with Gap
            // We start after the gap (Top Left direction from Top Right)
            // Clockwise: 
            // Top Right (Arrow location) -> Gap -> Top Right Corner Start
            
            // Actually, let's trace:
            // Arrow is at Top Edge, near Right Corner.
            // Arrow Points Right.
            // Line comes from Left.
            // So Line End is at Arrow Tail.
            // Line Start is after the Arrow Tip (and Gap).
            
            // Let's place Arrow Tip at `w - cornerRadius`.
            // So Gap is from `w - cornerRadius` to `w - cornerRadius + gap`?
            // Or better: Let's center the gap at `w - cornerRadius`.
            
            // Simplified Path:
            // Start: Top Right Corner (Start of curve)
            // MoveTo(w - cornerRadius + gap/2, 0) ? No, that's moving Right.
            
            // Let's draw:
            // Start at Right Loop (Down) -> Bottom -> Left -> Top.
            // Stop at Top Edge before the Gap.
            // Arrow at the end.
            // Start Point of loop is after the Gap.
            
            val arrowTipX = w - cornerRadius
            val arrowTipY = strokeWidth / 2
            
            val gapStart = arrowTipX + 2.dp.toPx() // Slightly right of the "corner start"
            // Actually, if arrow points Right, it points INTO the corner.
            // Correct.
            // So the Loop should starts IN the corner?
            // "Arrow and ending are not connected".
            // Implies Arrow is at End of Line.
            // And there is a gap between Arrow and Start of Line.
            
            // Let's start the line at `Right Edge` (Top part).
            // i.e. x = w, y = cornerRadius.
            // Trace Clockwise: Down -> Left -> Up -> Right.
            // End at Top Edge (x = arrowTipX - gap).
            
            val path = androidx.compose.ui.graphics.Path().apply {
                // Start at Top-Right Corner (vertical part)
                // We skip the top-right arc for the 'Start' to make a distinct gap if the arrow is *on* the top edge.
                // Let's start at angle 0 deg (Right, Middle of arc? No).
                
                // Let's simply leave the "Top Right Corner" empty or partially empty.
                
                // Move to "Right Edge, Top part"
                moveTo(w, cornerRadius)
                
                // Right Line
                lineTo(w, h - cornerRadius)
                
                // Bottom Right Corner
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(w - 2 * cornerRadius, h - 2 * cornerRadius, w, h),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                
                // Bottom Line
                lineTo(cornerRadius, h)
                
                // Bottom Left Corner
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(0f, h - 2 * cornerRadius, 2 * cornerRadius, h),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                
                // Left Line
                lineTo(0f, cornerRadius)
                
                // Top Left Corner
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(0f, 0f, 2 * cornerRadius, 2 * cornerRadius),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                
                // Top Line (Left to Right)
                // Stop before the arrow
                // Arrow is at `w - cornerRadius`.
                // Micro gap as requested (0.8dp)
                lineTo(w - cornerRadius - 0.8.dp.toPx(), 0f)
            }
            
            drawPath(
                path = path,
                color = color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
            
            // 2. Draw Arrow (Detached)
            // Position: At `w - cornerRadius` (End of top line conceptually), but we stopped short.
            // We draw the arrow head floating there.
            
            val arrowX = w - cornerRadius
            val arrowY = strokeWidth / 2
            val arrowSize = 3.dp.toPx()
            
            val arrowPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(arrowX - arrowSize, arrowY - arrowSize)
                lineTo(arrowX, arrowY)
                lineTo(arrowX - arrowSize, arrowY + arrowSize)
            }
            
            drawPath(
                path = arrowPath,
                color = color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
        
        // 3. Draw Text if present
        if (count != null) {
            Text(
                text = count,
                color = color,
                fontSize = if(count.length > 2) 8.sp else 10.sp, 
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                modifier = Modifier.align(Alignment.Center) 
            )
        }
    }
}

@Composable
fun RoundedShuffleIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeWidth = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        
        // Shuffle style: Two S-curves crossing
        // Top-Left to Bottom-Right
        // Bottom-Left to Top-Right
        
        val p1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h * 0.25f)
            cubicTo(
                w * 0.5f, h * 0.25f, // Control 1
                w * 0.5f, h * 0.75f, // Control 2
                w, h * 0.75f         // End
            )
        }
        
        val p2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h * 0.75f)
            cubicTo(
                w * 0.5f, h * 0.75f, 
                w * 0.5f, h * 0.25f, 
                w, h * 0.25f
            )
        }

        drawPath(p1, color, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawPath(p2, color, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        
        // Arrow heads at the right ends
        val arrowSize = 3.dp.toPx()
        
        // Arrow for Top-Right (End of P2)
        val arrowP2 = androidx.compose.ui.graphics.Path().apply {
             moveTo(w - arrowSize, h * 0.25f - arrowSize)
             lineTo(w, h * 0.25f)
             lineTo(w - arrowSize, h * 0.25f + arrowSize)
        }
        drawPath(arrowP2, color, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))

        // Arrow for Bottom-Right (End of P1)
        val arrowP1 = androidx.compose.ui.graphics.Path().apply {
             moveTo(w - arrowSize, h * 0.75f - arrowSize)
             lineTo(w, h * 0.75f)
             lineTo(w - arrowSize, h * 0.75f + arrowSize)
        }
        drawPath(arrowP1, color, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

data class NavItem(val name: String, val icon: ImageVector)

val NavItems = listOf(
    NavItem("音乐库", Icons.Default.Home),
    NavItem("歌单", Icons.Default.LibraryMusic),
    NavItem("我的", Icons.Default.Person)
)

