package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AgroCharcoalDark
import com.example.ui.theme.AgroGreenPrimary
import com.example.ui.theme.StatusEmergencyRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Composable
fun MjpegStreamPlayer(
    streamUrl: String,
    modifier: Modifier = Modifier,
    overlayContent: @Composable () -> Unit = {}
) {
    var currentFrame by remember { mutableStateOf<Bitmap?>(null) }
    var isConnecting by remember { mutableStateOf(true) }
    var streamError by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(streamUrl, retryTrigger) {
        isConnecting = true
        streamError = null
        withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(streamUrl)
                .build()

            var response: Response? = null
            try {
                response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        streamError = "HTTP ${response.code}: Unable to open camera stream"
                        isConnecting = false
                    }
                    return@withContext
                }

                val body = response.body
                if (body == null) {
                    withContext(Dispatchers.Main) {
                        streamError = "Camera stream response body is null"
                        isConnecting = false
                    }
                    return@withContext
                }

                val inputStream = BufferedInputStream(body.byteStream())
                withContext(Dispatchers.Main) {
                    isConnecting = false
                }

                readMjpegFrames(inputStream) { bitmap ->
                    if (coroutineContext.isActive) {
                        currentFrame = bitmap
                    }
                }
            } catch (e: Exception) {
                if (coroutineContext.isActive) {
                    withContext(Dispatchers.Main) {
                        streamError = e.localizedMessage ?: "Camera offline or unreachable"
                        isConnecting = false
                    }
                }
            } finally {
                response?.close()
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(12.dp)),
        color = AgroCharcoalDark,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val frame = currentFrame
            if (frame != null && streamError == null) {
                Image(
                    bitmap = frame.asImageBitmap(),
                    contentDescription = "Live ESP32-CAM MJPEG Video Stream",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (isConnecting) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = AgroGreenPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Connecting to ESP32-CAM...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            text = streamUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF141915)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = "Camera Offline",
                            tint = StatusEmergencyRed,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ESP32-CAM STREAM OFFLINE",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = streamError ?: "Target: $streamUrl",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA0B3A4)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { retryTrigger++ },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AgroGreenPrimary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Reconnect Stream")
                        }
                    }
                }
            }

            // Overlay controls or bounding boxes
            overlayContent()
        }
    }
}

/**
 * Parses MJPEG stream looking for JPEG SOI (0xFF, 0xD8) and EOI (0xFF, 0xD9) markers
 */
private fun readMjpegFrames(inputStream: InputStream, onFrameDecoded: (Bitmap) -> Unit) {
    val buffer = ByteArray(4096)
    val frameStream = ByteArrayOutputStream(32768)
    var prevByte = -1
    var insideJpeg = false

    var bytesRead: Int
    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
        for (i in 0 until bytesRead) {
            val b = buffer[i].toInt() and 0xFF

            if (!insideJpeg) {
                if (prevByte == 0xFF && b == 0xD8) {
                    // Start of Image
                    insideJpeg = true
                    frameStream.reset()
                    frameStream.write(0xFF)
                    frameStream.write(0xD8)
                }
            } else {
                frameStream.write(b)
                if (prevByte == 0xFF && b == 0xD9) {
                    // End of Image
                    insideJpeg = false
                    val jpegBytes = frameStream.toByteArray()
                    try {
                        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                        if (bitmap != null) {
                            onFrameDecoded(bitmap)
                        }
                    } catch (_: Exception) {
                    }
                    frameStream.reset()
                }
            }
            prevByte = b
        }
    }
}
