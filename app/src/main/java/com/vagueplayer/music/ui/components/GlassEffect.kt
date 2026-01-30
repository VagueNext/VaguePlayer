package com.vagueplayer.music.ui.components

import android.graphics.RenderEffect as nativeRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import android.graphics.Shader as nativeShader
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp // [FIX] Import
import androidx.compose.foundation.background // [FIX] Import
import androidx.compose.ui.draw.clip // [FIX] Import
import org.intellij.lang.annotations.Language // [FIX] Import

/**
 * Global Defaults for Liquid Glass Visuals
 */
object LiquidGlassDefaults {
    val BlurRadius = 0.dp
    val EdgeWidth = 30.0f // [Visual] Increased from 15f
    val DistortionStrength = 45.0f // [Visual] Increased from 15f
    val Tint = Color.White.copy(alpha = 0.2f) // [Visual] Increased from 0.02f for visibility
    val CornerRadius = 32.dp
}

/**
 * Unified Water Drop Glass Shader (Lens Filter Mode)
 * acts as a lens that distorts the content behind it within a specific region.
 */
@Language("AGSL")
val WaterDropGlassShader = """
    uniform shader content;
    uniform float2 uResolution;
    uniform float4 uBounds1; // Player
    uniform float4 uBounds2; // Nav Pill
    uniform float4 uBounds3; // Search Orb
    uniform float4 uBounds4; // [NEW] Transient Overlay (Menus/Dialogs)
    uniform float4 uCommonBounds; // Bounding box of all 4
    
    uniform float uCornerRadius; 
    uniform float uEdgeWidth;
    uniform float uDistortionStrength;
    uniform float uAberrationStrength; // [FIX] Missing uniform
    uniform float uFusionStrength; // [NEW] Controls the "stickiness" or surface tension
    layout(color) uniform half4 uTint;

    float sdRoundedBox(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + r;
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    }

    // Calculates SDF for a single box defined by rect [l, t, r, b]
    float boxSDF(float2 p, float4 rect, float r) {
        if (rect.x >= rect.z || rect.y >= rect.w) return 1000.0; // Invalid rect
        float2 size = float2(rect.z - rect.x, rect.w - rect.y);
        float2 center = float2(rect.x + size.x * 0.5, rect.y + size.y * 0.5);
        
        // [FIX] Clamp radius to ensure capsule/circle shape even if uniform is too large
        float safeRadius = min(r, min(size.x, size.y) * 0.5);
        
        return sdRoundedBox(p - center, size * 0.5, safeRadius);
    }

    // [NEW] Smooth Minimum (Metaball/Liquid Fusion)
    // k = fusion strength (smoothness factor)
    float smin(float a, float b, float k) {
        // Polynomial smin (classic)
        float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
        return mix(b, a, h) - k * h * (1.0 - h);
    }
    
    // Helper to sample combined SDF from all bounds
    float getCombinedDist(float2 p) {
        // [FIX] Combine 4 bounds using Smooth Min
        float d1 = boxSDF(p, uBounds1, uCornerRadius);
        float d2 = boxSDF(p, uBounds2, uCornerRadius);
        float d3 = boxSDF(p, uBounds3, uCornerRadius);
        float d4 = boxSDF(p, uBounds4, uCornerRadius);
        
        // Chain fusion
        // We fuse d1 (Player) with d2 (Nav) with d3 (Search) with d4 (Overlay)
        // Use a safe non-zero fusion strength to avoid division by zero or artifacts
        float k = max(0.1, uFusionStrength); 
        
        float d = smin(d1, d2, k);
        d = smin(d, d3, k);
        d = smin(d, d4, k);
        
        return d;
    }

    half4 main(float2 fragCoord) {
        // Optimization: Check common bounds + padding
        // Need slightly more padding for fusion field
        float padding = uEdgeWidth * 2.0 + uFusionStrength + 50.0;
        if (fragCoord.x < uCommonBounds.x - padding || fragCoord.x > uCommonBounds.z + padding ||
            fragCoord.y < uCommonBounds.y - padding || fragCoord.y > uCommonBounds.w + padding) {
            return content.eval(fragCoord);
        }

        // Combine SDFs (Union = min)
        float d = getCombinedDist(fragCoord);

        // If outside the glass shape, return original content immediately
        // [USER] Strict clipping: "Distortion only inside glass"
        if (d >= 0.0) {
            return content.eval(fragCoord);
        }

        // --- LIQUID GLASS PHYSICS ---

        // Normal Calculation (Finite Difference)
        float eps = 1.0;
        float dX = getCombinedDist(fragCoord + float2(eps, 0.0)) - getCombinedDist(fragCoord - float2(eps, 0.0));
        float dY = getCombinedDist(fragCoord + float2(0.0, eps)) - getCombinedDist(fragCoord - float2(0.0, eps));
        float2 normal = normalize(float2(dX, dY));

        // Edge Factor - how close to the edge are we?
        // d goes from negative (inside) to positive (outside).
        // smoothstep        // Edge Factor
        float influence = uEdgeWidth;
        float edgeFactor = smoothstep(-influence, 0.0, d);
        
        // Lens Profile: Curve more at edges
        float lensProfile = pow(edgeFactor, 3.0); // Steeper curve

        // [USER] STRICT DISTORTION CLIPPING
        // We ensure distortion is exactly ZERO if d >= 0.0
        // (Though the early exit above handles d>=0, this is double safety)
        float mask = 1.0 - step(0.0, d); 

        // Distortion (Magnify/Stretch)
        // [FIX] Invert direction: Sample from INSIDE (magnify), do not pull outside pixels in.
        // Previously: normal * ... (points out) -> samples outside.
        // Now: -normal * ... (points in) -> samples inside.
        float2 distortion = -normal * lensProfile * uDistortionStrength * mask;

        // Sample Position
        float2 samplePos = fragCoord + distortion;
        samplePos = clamp(samplePos, float2(1.0), uResolution - 1.0);

        // Chromatic Aberration
        float chromaStrength = uAberrationStrength * 10.0 * lensProfile;
        half4 color;
        color.r = content.eval(samplePos - normal * chromaStrength).r;
        color.g = content.eval(samplePos).g;
        color.b = content.eval(samplePos + normal * chromaStrength).b;
        color.a = content.eval(samplePos).a;

        // Specular Ridge (Lighting) - Modeled Physics
        float3 lightDir = normalize(float3(0.0, 0.0, 1.0)); // [FIX] Frontal light to avoid edge glare
        float3 viewDir = float3(0.0, 0.0, 1.0);
        float3 surfaceNormal = normalize(float3(normal * lensProfile * 2.0, 1.0)); 
 
        float NdotL = max(0.0, dot(surfaceNormal, lightDir));
        float specular = 0.0; // [FIX] Removed white glare completely
        
        // Fresnel/Rim Light
        float fresnel = 0.0; // [FIX] Removed rim light completely

        half3 finalColor = color.rgb;
        finalColor += specular;
        finalColor += fresnel;

        // Tint Mixing: Mix based on alpha
        // We want the glass to have a body color (uTint)
        // Mix ratio increases at edges or just uniform?
        // User asked for "white transparent color".
        finalColor = mix(finalColor, uTint.rgb, uTint.a);

        return half4(finalColor, color.a);
    }
"""

/**
 * Applies a Liquid Glass Lens effect to a container.
 * Supports up to 3 distinct separate glass islands.
 */
fun Modifier.liquidGlassLens(
    bounds1: Rect? = null,
    bounds2: Rect? = null,
    bounds3: Rect? = null,
    bounds4: Rect? = null, // [NEW] Transient Overlay
    cornerRadius: Dp = LiquidGlassDefaults.CornerRadius,
    distortionStrength: Float = LiquidGlassDefaults.DistortionStrength,
    edgeWidth: Float = LiquidGlassDefaults.EdgeWidth,
    aberrationStrength: Float = 1.0f,
    tint: Color = LiquidGlassDefaults.Tint,
    fusionStrength: Float = 25.0f, // [NEW] Default Fusion Strength
    enableShader: Boolean = true
): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !enableShader) {
        return@composed this
    }

    val density = androidx.compose.ui.platform.LocalDensity.current.density
    
    val shader = remember { RuntimeShader(WaterDropGlassShader) }
    
    this.graphicsLayer {
        clip = false
        
        // Update Shader Uniforms
        shader.setFloatUniform("uResolution", size.width, size.height)
        
        // Set Color (Premultiplied logic if needed, or just RGB)
        shader.setColorUniform("uTint", tint.toArgb())
        shader.setFloatUniform("uDistortionStrength", distortionStrength)
        shader.setFloatUniform("uEdgeWidth", edgeWidth)
        shader.setFloatUniform("uAberrationStrength", aberrationStrength)
        shader.setFloatUniform("uCornerRadius", cornerRadius.toPx())
        shader.setFloatUniform("uFusionStrength", fusionStrength) // [NEW] Pass to shader
        
        // Pass Bounds (x, y, w, h)
        // Slot 1
        if (bounds1 != null) {
            shader.setFloatUniform("uBounds1", bounds1.left, bounds1.top, bounds1.right, bounds1.bottom)
        } else {
            shader.setFloatUniform("uBounds1", 0f, 0f, 0f, 0f)
        }
        
        // Slot 2
        if (bounds2 != null) {
            shader.setFloatUniform("uBounds2", bounds2.left, bounds2.top, bounds2.right, bounds2.bottom)
        } else {
            shader.setFloatUniform("uBounds2", 0f, 0f, 0f, 0f)
        }

        // Slot 3
        if (bounds3 != null) {
            shader.setFloatUniform("uBounds3", bounds3.left, bounds3.top, bounds3.right, bounds3.bottom)
        } else {
            shader.setFloatUniform("uBounds3", 0f, 0f, 0f, 0f)
        }

        // Slot 4 (Overlay)
        if (bounds4 != null) {
            shader.setFloatUniform("uBounds4", bounds4.left, bounds4.top, bounds4.right, bounds4.bottom)
        } else {
            shader.setFloatUniform("uBounds4", 0f, 0f, 0f, 0f)
        }

        // Calculate Common Bounds (Union of all active bounds)
        var minX = 10000f; var minY = 10000f; var maxX = -10000f; var maxY = -10000f
        val activeBounds = listOfNotNull(bounds1, bounds2, bounds3, bounds4)
        if (activeBounds.isNotEmpty()) {
            activeBounds.forEach {
                minX = minOf(minX, it.left)
                minY = minOf(minY, it.top)
                maxX = maxOf(maxX, it.right)
                maxY = maxOf(maxY, it.bottom)
            }
            shader.setFloatUniform("uCommonBounds", minX, minY, maxX, maxY)
        } else {
            shader.setFloatUniform("uCommonBounds", 0f, 0f, 0f, 0f)
        }

        // Apply
        renderEffect = nativeRenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }
}

/**
 * Applies a simple self-contained Glass Effect to the component.
 * Uses the component's own size as the glass bounds.
 */
fun Modifier.simpleGlass(
    cornerRadius: Dp = LiquidGlassDefaults.CornerRadius,
    tint: Color = LiquidGlassDefaults.Tint,
    edgeWidth: Float = LiquidGlassDefaults.EdgeWidth,
    distortionStrength: Float = LiquidGlassDefaults.DistortionStrength,
    blurRadius: Dp = 0.dp, // [NEW] Supports Gaussian Blur
    aberrationStrength: Float = 0f,
    enableShader: Boolean = true
): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !enableShader) {
        return@composed this.background(tint, RoundedCornerShape(cornerRadius))
    }

    val shader = remember { RuntimeShader(WaterDropGlassShader) }
    val density = LocalDensity.current
    var size by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    val combinedEffect = remember(size, distortionStrength, blurRadius, tint) {
        if (size.width <= 0 || size.height <= 0) return@remember null

        val radiusPx = with(density) { cornerRadius.toPx() }
        val blurPx = with(density) { blurRadius.toPx() }
        val safeEdgeWidth = edgeWidth.coerceAtMost(radiusPx * 0.95f).coerceAtLeast(0f)

        shader.setFloatUniform("uResolution", size.width, size.height)
        val bounds = Rect(0f, 0f, size.width, size.height)
        
        shader.setFloatUniform("uCommonBounds", bounds.left, bounds.top, bounds.right, bounds.bottom)
        shader.setFloatUniform("uBounds1", bounds.left, bounds.top, bounds.right, bounds.bottom)
        shader.setFloatUniform("uBounds2", 0f, 0f, 0f, 0f)
        shader.setFloatUniform("uBounds3", 0f, 0f, 0f, 0f)
        shader.setFloatUniform("uBounds4", 0f, 0f, 0f, 0f)
        
        shader.setFloatUniform("uCornerRadius", radiusPx)
        shader.setFloatUniform("uEdgeWidth", safeEdgeWidth)
        shader.setFloatUniform("uDistortionStrength", distortionStrength)
        shader.setFloatUniform("uAberrationStrength", aberrationStrength)
        // [FIX] Pass transparent to shader to avoid "dirty" sampling artifacts.
        // The color is now handled by the .drawWithContent { } Glaze pass below.
        shader.setColorUniform("uTint", Color.Transparent.toArgb())

        val shaderEffect = nativeRenderEffect.createRuntimeShaderEffect(shader, "content")
        
        // combine with blur if requested
        if (blurPx > 0.01f) {
            val blurEffect = nativeRenderEffect.createBlurEffect(blurPx, blurPx, nativeShader.TileMode.CLAMP)
            nativeRenderEffect.createChainEffect(blurEffect, shaderEffect).asComposeRenderEffect()
        } else {
            shaderEffect.asComposeRenderEffect()
        }
    }

    this
        .onGloballyPositioned { size = androidx.compose.ui.geometry.Size(it.size.width.toFloat(), it.size.height.toFloat()) }
        .graphicsLayer {
            if (combinedEffect != null) {
                this.renderEffect = combinedEffect
                this.clip = true
            }
        }
        .drawWithContent {
            drawContent()
            // [GLAZE] Apply a clean white frost layer on top of the blurred/distorted content
            // This solves the "dirty/muddy" look at high blur/distortion levels.
            if (tint.alpha > 0f) {
                drawRect(
                    color = tint, 
                    blendMode = BlendMode.SrcAtop // [FIX] SrcAtop ensures tint only draws on top of existing non-transparent pixels, preventing white halo on blurred edges
                )
            }
        }
        .clip(RoundedCornerShape(cornerRadius))
}

