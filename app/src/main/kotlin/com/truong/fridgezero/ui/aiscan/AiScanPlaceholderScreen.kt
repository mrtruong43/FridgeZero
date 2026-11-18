package com.truong.fridgezero.ui.aiscan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.truong.fridgezero.R
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.CavitySlate
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LilacAccent
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite
import com.truong.fridgezero.ui.theme.dotGridBackground

/**
 * Phase-2 CameraX Receipt Scanner preview screen with contextual CAMERA permission handling.
 */
@Composable
fun AiScanPlaceholderScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var hasPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasPermission = granted
  }

  var torchEnabled by remember { mutableStateOf(false) }
  var boundCamera by remember { mutableStateOf<Camera?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .dotGridBackground()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Top Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      NeoBrutalistButton(
        onClick = onBack,
        containerColor = SurfaceWhite,
        cornerRadius = 10.dp,
        borderWidth = 2.dp,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
      ) {
        Text(
          text = stringResource(R.string.ai_scan_back),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          color = InkBlack
        )
      }

      Text(
        text = stringResource(R.string.ai_scan_title),
        fontFamily = DisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        color = InkBlack
      )

      StickerBadge(
        text = stringResource(R.string.home_tab_scan_badge),
        containerColor = LilacAccent,
        tiltDegrees = 2f,
        mono = true
      )
    }

    // Center Viewfinder / Permission Card
    NeoBrutalistCard(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      containerColor = if (hasPermission) CavitySlate else SurfaceWhite
    ) {
      if (hasPermission) {
        CameraViewfinderBox(
          torchEnabled = torchEnabled,
          onCameraBound = { boundCamera = it },
          modifier = Modifier.fillMaxSize()
        )
      } else {
        Column(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text(text = "📷", fontSize = 48.sp)
          Spacer(Modifier.height(12.dp))
          Text(
            text = stringResource(R.string.ai_scan_permission_title),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = InkBlack,
            textAlign = TextAlign.Center
          )
          Spacer(Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.ai_scan_permission_body),
            fontFamily = BodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = InkBlack.copy(alpha = 0.78f),
            textAlign = TextAlign.Center
          )
          Spacer(Modifier.height(18.dp))
          NeoBrutalistButton(
            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            containerColor = LimeFresh
          ) {
            Text(
              text = stringResource(R.string.ai_scan_permission_grant),
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 14.sp,
              color = InkBlack
            )
          }
        }
      }
    }

    // Phase 2 Banner
    NeoBrutalistCard(
      modifier = Modifier.fillMaxWidth(),
      containerColor = LemonWarn,
      cornerRadius = 14.dp,
      shadowOffset = 3.dp
    ) {
      Text(
        text = stringResource(R.string.ai_scan_banner),
        fontFamily = BodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        color = InkBlack,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)
      )
    }

    // Bottom Action Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      NeoBrutalistButton(
        onClick = {
          val next = !torchEnabled
          torchEnabled = next
          boundCamera?.cameraControl?.enableTorch(next)
        },
        enabled = hasPermission && boundCamera != null,
        modifier = Modifier.weight(1f),
        containerColor = if (torchEnabled) LemonWarn else SurfaceWhite
      ) {
        Text(
          text = stringResource(if (torchEnabled) R.string.ai_scan_flash_on else R.string.ai_scan_flash_off),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          color = InkBlack
        )
      }

      NeoBrutalistButton(
        onClick = {},
        enabled = false,
        modifier = Modifier.weight(1.4f),
        containerColor = LimeFresh
      ) {
        Text(
          text = stringResource(R.string.ai_scan_capture_gd2),
          fontFamily = DisplayFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 14.sp,
          color = InkBlack
        )
      }
    }
  }
}

@Composable
private fun CameraViewfinderBox(
  torchEnabled: Boolean,
  onCameraBound: (Camera?) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val previewView = remember {
    PreviewView(context).apply {
      scaleType = PreviewView.ScaleType.FILL_CENTER
    }
  }
  var cameraError by remember { mutableStateOf(false) }

  DisposableEffect(lifecycleOwner, previewView) {
    val future = ProcessCameraProvider.getInstance(context)
    var provider: ProcessCameraProvider? = null
    val listener = Runnable {
      try {
        val camProvider = future.get()
        provider = camProvider
        val preview = Preview.Builder().build().also {
          it.surfaceProvider = previewView.surfaceProvider
        }
        camProvider.unbindAll()
        val cam = camProvider.bindToLifecycle(
          lifecycleOwner,
          CameraSelector.DEFAULT_BACK_CAMERA,
          preview
        )
        cam.cameraControl.enableTorch(torchEnabled)
        onCameraBound(cam)
        cameraError = false
      } catch (_: Exception) {
        cameraError = true
        onCameraBound(null)
      }
    }
    future.addListener(listener, ContextCompat.getMainExecutor(context))
    onDispose {
      provider?.unbindAll()
      onCameraBound(null)
    }
  }

  val transition = rememberInfiniteTransition(label = "scanBeam")
  val beamProgress by transition.animateFloat(
    initialValue = 0.14f,
    targetValue = 0.86f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "beamY"
  )

  Box(modifier = modifier.background(CavitySlate)) {
    if (!cameraError) {
      AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
      )
    } else {
      Text(
        text = stringResource(R.string.ai_scan_camera_unavailable),
        fontFamily = BodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        color = Color.White.copy(alpha = 0.82f),
        textAlign = TextAlign.Center,
        modifier = Modifier.align(Alignment.Center).padding(28.dp)
      )
    }

    // 4 receipt finder corner brackets + LimeFresh laser scan line
    Canvas(modifier = Modifier.fillMaxSize()) {
      val insetX = size.width * 0.12f
      val insetY = size.height * 0.14f
      val left = insetX
      val right = size.width - insetX
      val top = insetY
      val bottom = size.height - insetY
      val bracket = 28.dp.toPx()
      val stroke = 4.dp.toPx()

      // Top-left
      drawLine(LimeFresh, Offset(left, top), Offset(left + bracket, top), stroke, StrokeCap.Square)
      drawLine(LimeFresh, Offset(left, top), Offset(left, top + bracket), stroke, StrokeCap.Square)
      // Top-right
      drawLine(LimeFresh, Offset(right - bracket, top), Offset(right, top), stroke, StrokeCap.Square)
      drawLine(LimeFresh, Offset(right, top), Offset(right, top + bracket), stroke, StrokeCap.Square)
      // Bottom-left
      drawLine(LimeFresh, Offset(left, bottom - bracket), Offset(left, bottom), stroke, StrokeCap.Square)
      drawLine(LimeFresh, Offset(left, bottom), Offset(left + bracket, bottom), stroke, StrokeCap.Square)
      // Bottom-right
      drawLine(LimeFresh, Offset(right - bracket, bottom), Offset(right, bottom), stroke, StrokeCap.Square)
      drawLine(LimeFresh, Offset(right, bottom - bracket), Offset(right, bottom), stroke, StrokeCap.Square)

      // Animated horizontal scan laser
      val beamY = size.height * beamProgress
      drawLine(
        color = LimeFresh,
        start = Offset(left + 8.dp.toPx(), beamY),
        end = Offset(right - 8.dp.toPx(), beamY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
      )
    }
  }
}
