package com.erz.zoomablecanvas

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.erz.zoomablecanvas.lib.ZoomableCanvas
import com.erz.zoomablecanvas.lib.ZoomableConfiguration
import com.erz.zoomablecanvas.lib.ZoomableManager
import com.erz.zoomablecanvas.ui.theme.ZoomableCanvasTheme

/**
 * Todos - Debug drawer with options
 * pager example
 * drawing example
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZoomableCanvasTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ZoomableCanvas(
                        modifier = Modifier,
                        configuration = ZoomableConfiguration(
                            canFling = true,
                            canPan = true,
                            canDoubleTapToZoom = true,
                            canLongPress = true,
                            constraintToComposableBounds = true,
                            enableMiniMap = true
                        ),
                        manager = object: ZoomableManager(){
                            override fun onDraw(canvas: Canvas) {
                                canvas.drawCircle(500f, 750f, 50f, Paint().apply {
                                    strokeWidth = 5f
                                    style = Paint.Style.STROKE
                                })
                            }
                        }
                    )
                }
            }
        }
    }
}