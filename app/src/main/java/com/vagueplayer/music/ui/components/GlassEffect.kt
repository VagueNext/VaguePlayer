package com.vagueplayer.music.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb // Fix: Import toArgb extension
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import org.intellij.lang.annotations.Language

/**
 * Unified Water Drop Glass Shader
 * Profile: Refined Thin Lip (Edge=25.0, Curve=Cubic, Str=25.0)
 */
@Language("AGSL")
val WaterDropGlassShader = """
    uniform shader content;
    uniform float2 uResolution;
    uniform float2 uSize;
    uniform float uCornerRadius;
    uniform float uEdgeWidth; 
    uniform float uDistortionStrength;
    uniform float uAberrationStrength; // NEW: Control Rainbow Strength separately
    layout(color) uniform half4 uTint;

    float sdRoundedBox(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + r;
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    }

    half4 main(float2 fragCoord) {
        float2 center = uSize * 0.5;
        float2 localP = fragCoord - center;
        
        // Standard SDF
        float d = sdRoundedBox(localP, center, uCornerRadius);
        
        if (d > 0.0) return half4(0.0); // Clip outside

        // --- 1. COMPUTE SDF GRADIENT (Surface Normal) ---
        float eps = 1.0;
        float dX = sdRoundedBox(localP + float2(eps, 0.0), center, uCornerRadius) -
                   sdRoundedBox(localP - float2(eps, 0.0), center, uCornerRadius);
        float dY = sdRoundedBox(localP + float2(0.0, eps), center, uCornerRadius) -
                   sdRoundedBox(localP - float2(0.0, eps), center, uCornerRadius);
        float2 sdfNormal = normalize(float2(dX, dY) + 0.0001);

        // --- 2. DISTANCE FIELD & EDGE MASK ---
        // The 'd' variable is already calculated above.
        // CRITICAL FIX: Restrict influence strictly to the edge width.
        // Previously max(Radius*2) caused the gradient to bleed into the center.
        float influence = uEdgeWidth; 
        
        // edgeT: 0.0 (Center/Body) -> 1.0 (Edge/Border)
        float edgeT = smoothstep(-influence, 0.0, d); 

        // --- 3. REFRACTION CALCULATION (EDGE-ONLY MAGNIFICATION) ---
        
        // Normalize coordinates (-1 to 1)
        float2 uv = (fragCoord - center) / (uSize * 0.5);
        
        // Usage of edgeT to mask the distortion
        // distortionAmount increases as we get closer to the edge
        // pow(edgeT, 3.0) Creates a smoother, wider curve (REALISTIC LENS)
        float mask = pow(edgeT, 3.0);
        
        // Strength: Increased slightly as requested (0.005 -> 0.01)
        float strength = uDistortionStrength * 0.01; 
        
        // Mag Factor: 1.0 (No change) -> <1.0 (Magnify) at edges
        float f = 1.0 - (mask * strength); 
        
        // Apply Distortion (Sample CLOSER to center -> Magnify)
        float2 uvDistorted = uv * f;
        
        // Remap back to pixels
        float2 posDistorted = uvDistorted * (uSize * 0.5) + center;
        float2 totalDistortion = posDistorted - fragCoord;
        
        float2 sampleUV = fragCoord + totalDistortion; 
        
        // --- 4. EDGE CLAMPING ---
        float2 safeMin = float2(1.0);
        float2 safeMax = uSize - 1.0;
        sampleUV = clamp(sampleUV, safeMin, safeMax);
        
        // --- 5. SAMPLE CONTENT ---
        half4 sampledColor = content.eval(sampleUV); 
        
        // --- 6. EDGE HIGHLIGHTS (Fresnel/Specular) ---
        // REMOVED: User requested to remove white highlights
        float rim = 0.0; 
        
        // --- 7. CHROMATIC ABERRATION ---
        // Split RGB along the radial direction
        // Multiplier increased (3.0 -> 15.0) for visible rainbow effect at edges (Prism)
        float chromaDist = uAberrationStrength * 15.0 * (f - 1.0); 
        float2 chromaOffset = normalize(uv) * chromaDist;
        
        half r_channel = content.eval(clamp(sampleUV + chromaOffset, safeMin, safeMax)).r;
        half b_channel = content.eval(clamp(sampleUV - chromaOffset, safeMin, safeMax)).b;
        
        // Reassemble
        half3 baseColor = half3(r_channel, sampledColor.g, b_channel) + rim; 
        
        // --- 8. APPLY TINT ---
        baseColor = mix(baseColor, uTint.rgb, uTint.a);

        return half4(baseColor, sampledColor.a);
    }
""".trimIndent()

/**
 * Global Defaults for Liquid Glass Visuals
 */
object LiquidGlassDefaults {
    val BlurRadius = 0.dp 
    val EdgeWidth = 8.0f // [USER-REQUEST] 8.0
    val DistortionStrength = 6.0f // [USER-REQUEST] 6.0
    val Tint = Color.White.copy(alpha = 0.35f) // [USER-REQUEST] Added 15% to 20% = 35% White
    val CornerRadius = 32.dp
}

/**
 * Applies the Unified Water Drop Glass Effect.
 * @param hazeState The HazeState from the root background.
 * @param cornerRadius The radius of the glass shape.
 */
fun Modifier.waterDropGlass(
    hazeState: HazeState?,
    cornerRadius: Dp = LiquidGlassDefaults.CornerRadius,
    blurRadius: Dp = LiquidGlassDefaults.BlurRadius,
    tint: Color = LiquidGlassDefaults.Tint,
    edgeWidth: Float = LiquidGlassDefaults.EdgeWidth,
    distortionStrength: Float = LiquidGlassDefaults.DistortionStrength,
    aberrationStrength: Float = distortionStrength * 0.05f, 
    enableShader: Boolean = true, 
    time: Float = 0f 
): Modifier = composed {
    val shader = remember { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(WaterDropGlassShader) 
        } else null
    }
    
    val density = LocalDensity.current
    var size by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    
    // CRITICAL FIX: Assign the RenderEffect to a variable!
    val renderEffect: androidx.compose.ui.graphics.RenderEffect? = 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null && size.width > 0 && enableShader) {
            val radiusPx = with(density) { cornerRadius.toPx() }
            val safeEdgeWidth = edgeWidth.coerceAtMost(radiusPx * 0.95f).coerceAtLeast(0f)
        
            shader.setFloatUniform("uResolution", size.width, size.height)
            shader.setFloatUniform("uSize", size.width, size.height)
            shader.setFloatUniform("uCornerRadius", radiusPx)
            shader.setFloatUniform("uEdgeWidth", safeEdgeWidth)
            shader.setFloatUniform("uDistortionStrength", distortionStrength)
            shader.setFloatUniform("uAberrationStrength", aberrationStrength)
            shader.setColorUniform("uTint", tint.toArgb())
            
            RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        } else {
            null
        }
    
    val shape = RoundedCornerShape(cornerRadius)
    
    this
        .onGloballyPositioned { size = androidx.compose.ui.geometry.Size(it.size.width.toFloat(), it.size.height.toFloat()) }
        .then(
            if (hazeState != null && enableShader) {
                // CORRECT ORDER: GraphicsLayer (Distortion) -> HazeChild (Content)
                // The RenderEffect must wrap the HazeChild to distort the blurred background.
                Modifier.graphicsLayer {
                    if (renderEffect != null) {
                        this.renderEffect = renderEffect
                        this.shape = shape
                        this.clip = true
                    }
                    alpha = 0.99f 
                }.hazeChild(
                    state = hazeState,
                    shape = shape,
                    style = HazeStyle(
                        backgroundColor = tint,
                        tint = HazeTint(tint), 
                        blurRadius = blurRadius,
                        fallbackTint = HazeTint(tint) 
                    )
                )
            } else {
                 Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.60f), 
                            Color.White.copy(alpha = 0.55f)
                        )
                    ),
                    shape = shape
                ).graphicsLayer {
                    if (renderEffect != null) {
                        this.renderEffect = renderEffect
                        this.shape = shape
                        this.clip = true
                    }
                    alpha = 0.99f 
                }
            }
        )
}
