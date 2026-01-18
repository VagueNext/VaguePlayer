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
        // --- LIQUID GLASS V2 SHADER (visionOS Style) ---
        float2 center = uSize * 0.5;
        float2 localP = fragCoord - center;
        
        // 1. SDF & NORMAL
        float d = sdRoundedBox(localP, center, uCornerRadius);
        if (d > 0.0) return half4(0.0); // Clip
        
        float eps = 1.0;
        float dX = sdRoundedBox(localP + float2(eps, 0.0), center, uCornerRadius) -
                   sdRoundedBox(localP - float2(eps, 0.0), center, uCornerRadius);
        float dY = sdRoundedBox(localP + float2(0.0, eps), center, uCornerRadius) -
                   sdRoundedBox(localP - float2(0.0, eps), center, uCornerRadius);
        float2 normal = normalize(float2(dX, dY));
        
        // 2. REFRACTION (Liquid Flow)
        // We use the Normal vector to drive the displacement, simulating a curved lens surface.
        // influence: How far the curve extends inwards.
        float influence = uEdgeWidth; 
        float edgeFactor = smoothstep(-influence, 0.0, d); // 0(Inner) -> 1(Edge)
        // Non-linear bulge profile for "surface tension" look
        float lensProfile = pow(edgeFactor, 2.5); 
        
        // Distortion Vector: Push pixels *away* from center (magnify) based on lens curvature
        // The track "Stretching" comes from this gradient.
        float2 distortion = normal * lensProfile * uDistortionStrength * 0.8;
       
        // Sample UV
        float2 samplePos = fragCoord + distortion;
        samplePos = clamp(samplePos, float2(1.0), uSize - 1.0); // Clamp to avoid bleeding
        
        // 3. CHROMATIC ABERRATION (Spectral Edges)
        float chromaStrength = uAberrationStrength * 8.0 * lensProfile;
        half4 color;
        color.r = content.eval(samplePos - normal * chromaStrength).r;
        color.g = content.eval(samplePos).g;
        color.b = content.eval(samplePos + normal * chromaStrength).b;
        color.a = 1.0;
        
        // 4. SPECULARITY (Ridge Light)
        // Simulating a top-down light source reflecting off the top curve
        float3 lightDir = normalize(float3(0.0, -0.8, 0.5)); // Top-ish light
        float3 viewDir = float3(0.0, 0.0, 1.0);
        float3 surfaceNormal = normalize(float3(normal * lensProfile, 1.0)); // Approximated 3D normal
        
        // Specular Ridge: Sharp reflection line
        float NdotL = max(0.0, dot(surfaceNormal, lightDir));
        float specular = pow(NdotL, 30.0) * 0.8; // High sharpness
        
        // Rim Light: Edge glow
        float fresnel = pow(1.0 - max(0.0, dot(surfaceNormal, viewDir)), 4.0) * 0.5;
        
        // 5. INNER SHADOW / CONTRAST (Volume)
        // Darken the "thick" parts of the glass slightly to give volume
        float volume = smoothstep(0.0, -influence, d) * 0.1; 
        
        // COMPOSE
        half3 finalColor = color.rgb;
        finalColor += specular; // Add shine
        finalColor += fresnel; // Add glow
        finalColor -= volume; // Subtract volume (ambient occlusion)
        
        // Mix Tint
        finalColor = mix(finalColor, uTint.rgb, uTint.a);
        
        return half4(finalColor, 1.0);
    }
""".trimIndent()

/**
 * Global Defaults for Liquid Glass Visuals
 */
object LiquidGlassDefaults {
    val BlurRadius = 0.dp // [USER-REQUEST] Remove Frosted Glass (Clear)
    val EdgeWidth = 15.0f 
    val DistortionStrength = 15.0f 
    val Tint = Color.White.copy(alpha = 0.02f) // [FIX] Max Transparency (2%)
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
    aberrationStrength: Float = distortionStrength * 0.05f, // [REVERT] Restore volume
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
