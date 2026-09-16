package com.jiale.liquidglass.sample

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jiale.liquidglass.GlassBackdrop
import com.jiale.liquidglass.GlassDefaults
import com.jiale.liquidglass.GlassParams
import com.jiale.liquidglass.LiquidGlassSurface
import com.jiale.liquidglass.LocalGlassParams
import com.jiale.liquidglass.RefractMode
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 最小示例：一张玻璃卡片 + 一块能拖动的玻璃 + 一个玻璃按钮 + 一个折射强度滑块。
 *
 * 背板是**代码画出来的**（渐变 + 几个色块），不需要任何图片资源 —— 换成你自己的图时，
 * 把 [createBackdrop] 换成 `rememberBackdropImage(File(...))` 就行。
 *
 * 色块是刻意的：**位移类的效果在平坦的纯色上看不出来**，有明暗层次才看得清折射和色散。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SampleScreen()
            }
        }
    }
}

@Composable
private fun SampleScreen() {
    val backdrop = remember { createBackdrop() }
    var refractDp by remember { mutableFloatStateOf(GlassDefaults.REFRACT_DP) }

    // 参数一次下发，**这棵子树里所有玻璃一起变**（不用逐个传参）
    val params = remember(refractDp) {
        GlassParams(refractDp = refractDp, refractMode = RefractMode.FIELD)
    }

    CompositionLocalProvider(LocalGlassParams provides params) {
        GlassBackdrop(image = backdrop) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = "LiquidGlass",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )

                // 1) 大玻璃：默认参数（模糊 12dp，边缘光宽度跟随 GlassParams）
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(Modifier.align(Alignment.CenterStart).padding(20.dp)) {
                        Text("表面玻璃", fontWeight = FontWeight.Medium)
                        Text(
                            text = "玻璃里是自己身后的模糊，不是半透明色块",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                // 2) 可拖动的玻璃：滑动时玻璃里的内容跟着位移 —— 这是"真玻璃在动"
                DraggableProbe()

                // 3) 小控件：模糊半径与边缘光都要收窄，否则只剩一团色块
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LiquidGlassSurface(
                        modifier = Modifier.size(180.dp, 40.dp),
                        shape = RoundedCornerShape(percent = 50),
                        blurRadius = GlassDefaults.COMPACT_BLUR,
                        edgeWidth = GlassDefaults.COMPACT_EDGE_WIDTH,
                        onClick = { },
                    ) {
                        Text("玻璃按钮", Modifier.align(Alignment.Center))
                    }
                }

                // 4) 实时调折射强度：效果面积是跟着强度走的
                Text("折射 ${String.format(Locale.US, "%.1f", refractDp)} dp")
                Slider(
                    value = refractDp,
                    onValueChange = { refractDp = it },
                    valueRange = GlassDefaults.REFRACT_DP_MIN..GlassDefaults.REFRACT_DP_MAX,
                )
                Text(
                    text = "拖到 0 看看：折射带会跟着收窄到没有 —— 效果面积是跟着强度走的。",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun DraggableProbe() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        val density = LocalDensity.current
        val probeW = with(density) { 170.dp.toPx() }
        val probeH = with(density) { 110.dp.toPx() }
        val maxX = (with(density) { maxWidth.toPx() } - probeW).coerceAtLeast(0f)
        val maxY = (with(density) { maxHeight.toPx() } - probeH).coerceAtLeast(0f)
        var pos by remember { mutableStateOf(Offset.Zero) }

        // 尺寸变了要把位置夹回框里，否则一出界就"拖不回来"
        LaunchedEffect(maxX, maxY) {
            pos = Offset(pos.x.coerceIn(0f, maxX), pos.y.coerceIn(0f, maxY))
        }

        LiquidGlassSurface(
            modifier = Modifier
                .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                .size(170.dp, 110.dp)
                // ⚠️ pointerInput 的 key 必须带上边界：它的 lambda 只在 key 变化时重建，
                //    写死 key 就会一直用第一次捕获的尺寸去夹位置
                .pointerInput(maxX, maxY) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        pos = Offset(
                            (pos.x + drag.x).coerceIn(0f, maxX),
                            (pos.y + drag.y).coerceIn(0f, maxY),
                        )
                    }
                },
            shape = MaterialTheme.shapes.large,
        ) {
            Text("拖动我", Modifier.align(Alignment.Center))
        }
    }
}

/** 代码画一张背板：一张渐变 + 几个半透明色块，用来让折射 / 色散看得见。 */
private fun createBackdrop(width: Int = 1080, height: Int = 2000): ImageBitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val gradient = Paint(Paint.ANTI_ALIAS_FLAG)
    gradient.shader = LinearGradient(
        0f, 0f, width.toFloat(), height.toFloat(),
        intArrayOf(0xFF0E1B33.toInt(), 0xFF3D2C8D.toInt(), 0xFFB33F6B.toInt(), 0xFFF2A65A.toInt()),
        floatArrayOf(0f, 0.45f, 0.8f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradient)

    val blob = Paint(Paint.ANTI_ALIAS_FLAG)
    fun circle(fx: Float, fy: Float, radius: Float, color: Int) {
        blob.color = color
        canvas.drawCircle(width * fx, height * fy, radius, blob)
    }
    circle(0.22f, 0.14f, 240f, 0x55FFFFFF.toInt())
    circle(0.78f, 0.24f, 180f, 0x33FFFFFF.toInt())
    circle(0.35f, 0.44f, 300f, 0x33000000)
    circle(0.85f, 0.56f, 220f, 0x44FFFFFF.toInt())
    circle(0.15f, 0.72f, 260f, 0x40000000)
    circle(0.62f, 0.86f, 200f, 0x55FFFFFF.toInt())

    return bitmap.asImageBitmap()
}
