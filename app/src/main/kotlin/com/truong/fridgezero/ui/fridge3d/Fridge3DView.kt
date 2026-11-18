package com.truong.fridgezero.ui.fridge3d

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.graphics.Outline
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.truong.fridgezero.data.CompartmentEntity
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "Fridge3D"
private const val PROJECTION_EPS = 0.005f
private const val DEFAULT_SURFACE_BG = "#FAF8F5"

data class DoorProjection(
  val xFrac: Float,
  val yFrac: Float,
  val visible: Boolean
)

data class StageBoundsPx(
  val left: Int = 0,
  val top: Int = 0,
  val width: Int = 0,
  val height: Int = 0
) {
  val isValid: Boolean get() = width > 0 && height > 0
}

@Stable
class Fridge3DStageController {
  var rootCoordinates: LayoutCoordinates? = null

  var activeToken by mutableStateOf<Any?>(null)
    private set
  var boundsPx by mutableStateOf(StageBoundsPx())
    private set
  var colorKey by mutableStateOf("inox")
    private set
  var compartments by mutableStateOf<List<CompartmentEntity>>(emptyList())
    private set
  var template by mutableStateOf("top_freezer")
    private set
  var openCompartmentId by mutableStateOf<String?>(null)
    private set
  var playWelcomeSpin by mutableStateOf(false)
    private set
  var showStickers by mutableStateOf(false)
    private set
  var surfaceBg by mutableStateOf(DEFAULT_SURFACE_BG)
    private set
  var cornerRadius by mutableStateOf(0.dp)
    private set
  var backdropColor by mutableStateOf<Color?>(null)
    private set
  var resetNonce by mutableIntStateOf(0)
    private set
  var itemAddedPulseNonce by mutableIntStateOf(0)
    private set

  internal var onWelcomeSpinConsumed: () -> Unit = {}
  internal var onDoorClick: ((String) -> Unit)? = null
  internal var onProjectionsUpdated: ((Map<String, DoorProjection>) -> Unit)? = null

  val isVisible: Boolean
    get() = activeToken != null && boundsPx.isValid

  fun updateSlot(
    token: Any,
    colorKey: String,
    compartments: List<CompartmentEntity>,
    template: String,
    openCompartmentId: String?,
    playWelcomeSpin: Boolean,
    showStickers: Boolean,
    surfaceBg: String,
    cornerRadius: Dp,
    backdropColor: Color?,
    onWelcomeSpinConsumed: () -> Unit,
    onDoorClick: ((String) -> Unit)?,
    onProjectionsUpdated: ((Map<String, DoorProjection>) -> Unit)?
  ) {
    activeToken = token
    this.colorKey = colorKey
    this.compartments = compartments
    this.template = template
    this.openCompartmentId = openCompartmentId
    this.playWelcomeSpin = playWelcomeSpin
    this.showStickers = showStickers
    this.surfaceBg = surfaceBg
    this.cornerRadius = cornerRadius
    this.backdropColor = backdropColor
    this.onWelcomeSpinConsumed = onWelcomeSpinConsumed
    this.onDoorClick = onDoorClick
    this.onProjectionsUpdated = onProjectionsUpdated
  }

  fun updateBounds(token: Any, bounds: StageBoundsPx) {
    if (activeToken === token && bounds.isValid && boundsPx != bounds) {
      boundsPx = bounds
    }
  }

  fun requestResetOrbit() {
    resetNonce += 1
  }

  fun requestItemAddedBounce() {
    itemAddedPulseNonce += 1
  }

  fun releaseSlot(token: Any) {
    if (activeToken === token) {
      activeToken = null
      boundsPx = StageBoundsPx()
      onDoorClick = null
      onProjectionsUpdated = null
    }
  }
}

@Composable
fun rememberFridge3DStageController(): Fridge3DStageController = remember { Fridge3DStageController() }

val LocalFridge3DStageController = staticCompositionLocalOf<Fridge3DStageController> {
  error("Fridge3DStageController is not provided in CompositionLocal")
}

@Keep
internal class FridgeJsBridge(
  private val onDoorTappedCallback: (String) -> Unit,
  private val onStickersProjectedCallback: (String) -> Unit,
  private val onWelcomeSpinFinishedCallback: () -> Unit
) {
  @JavascriptInterface
  fun onDoorTapped(compartmentId: String) = onDoorTappedCallback(compartmentId)

  @JavascriptInterface
  fun onStickersProjected(json: String) = onStickersProjectedCallback(json)

  @JavascriptInterface
  fun onWelcomeSpinFinished() = onWelcomeSpinFinishedCallback()
}

/** Optional framed backdrop (e.g. `SurfaceWhite` card face in Designer) drawn strictly behind the WebView. */
@Composable
fun Fridge3DStageBackdrop(
  controller: Fridge3DStageController,
  modifier: Modifier = Modifier
) {
  val backdrop = controller.backdropColor ?: return
  if (!controller.isVisible || !controller.boundsPx.isValid) return
  val b = controller.boundsPx
  val density = LocalDensity.current
  val shape = if (controller.cornerRadius > 0.dp) RoundedCornerShape(controller.cornerRadius) else RectangleShape
  Box(
    modifier = modifier
      .offset { IntOffset(b.left, b.top) }
      .size(with(density) { b.width.toDp() }, with(density) { b.height.toDp() })
      .background(backdrop, shape)
  )
}

/**
 * Single shared WebView host mounted once at [com.truong.fridgezero.MainActivity] root.
 * Only destroyed when the Activity leaves composition or recovers from renderer crash.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SharedFridge3DWebView(
  controller: Fridge3DStageController,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val density = LocalDensity.current
  val mainHandler = remember { Handler(Looper.getMainLooper()) }
  val latestHaptic by rememberUpdatedState(haptic)

  val projectionCache = remember { HashMap<String, DoorProjection>() }
  var pageReady by remember { mutableStateOf(false) }
  var recoveryKey by remember { mutableIntStateOf(0) }
  var lastValidBounds by remember { mutableStateOf(StageBoundsPx()) }

  LaunchedEffect(controller.activeToken, recoveryKey) {
    projectionCache.clear()
  }

  LaunchedEffect(controller.boundsPx) {
    if (controller.boundsPx.isValid) {
      lastValidBounds = controller.boundsPx
    }
  }

  val configJson = remember(
    controller.template,
    controller.colorKey,
    controller.compartments,
    controller.openCompartmentId,
    controller.showStickers,
    controller.surfaceBg
  ) {
    buildConfigJson(
      template = controller.template,
      colorKey = ShellColors.normalizeKey(controller.colorKey),
      compartments = controller.compartments,
      openCompartmentId = controller.openCompartmentId,
      showStickers = controller.showStickers,
      surfaceBg = controller.surfaceBg
    )
  }

  val webViewDestroyed = remember(recoveryKey) { AtomicBoolean(false) }

  val webView = remember(context, recoveryKey) {
    WebView.setWebContentsDebuggingEnabled(true)
    val jsBridge = FridgeJsBridge(
      onDoorTappedCallback = { compartmentId ->
        mainHandler.post {
          if (controller.isVisible) {
            controller.onDoorClick?.invoke(compartmentId)
          }
        }
      },
      onStickersProjectedCallback = { json ->
        val parsed = parseProjections(json)
        mainHandler.post {
          val listener = controller.onProjectionsUpdated
          if (!controller.isVisible || !controller.showStickers || listener == null) {
            return@post
          }
          var changed = false
          val removed = projectionCache.keys - parsed.keys
          if (removed.isNotEmpty()) {
            removed.forEach { projectionCache.remove(it) }
            changed = true
          }
          parsed.forEach { (id, next) ->
            val prev = projectionCache[id]
            if (
              prev == null ||
              prev.visible != next.visible ||
              abs(prev.xFrac - next.xFrac) > PROJECTION_EPS ||
              abs(prev.yFrac - next.yFrac) > PROJECTION_EPS
            ) {
              projectionCache[id] = next
              changed = true
            }
          }
          if (changed) {
            listener(projectionCache.toMap())
          }
        }
      },
      onWelcomeSpinFinishedCallback = {
        mainHandler.post {
          if (!webViewDestroyed.get()) {
            runCatching {
              latestHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
          }
        }
      }
    )

    WebView(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      setBackgroundColor(parseSurfaceColorInt(DEFAULT_SURFACE_BG))
      isVerticalScrollBarEnabled = false
      isHorizontalScrollBarEnabled = false
      overScrollMode = View.OVER_SCROLL_NEVER
      isHapticFeedbackEnabled = false

      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = false
        @Suppress("DEPRECATION")
        allowFileAccess = true
        builtInZoomControls = false
        displayZoomControls = false
        setSupportZoom(false)
        cacheMode = WebSettings.LOAD_NO_CACHE
      }

      addJavascriptInterface(jsBridge, "AndroidBridge")

      webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
          if (consoleMessage != null) {
            val text = "${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
            when (consoleMessage.messageLevel()) {
              ConsoleMessage.MessageLevel.ERROR -> Log.e(TAG, text)
              ConsoleMessage.MessageLevel.WARNING -> Log.w(TAG, text)
              else -> Log.d(TAG, text)
            }
          }
          return true
        }
      }

      webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
          pageReady = true
        }

        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
          Log.e(TAG, "renderer gone, didCrash=${detail?.didCrash()}")
          pageReady = false
          recoveryKey += 1
          return true
        }
      }

      loadUrl("file:///android_asset/fridge3d/index.html")
    }
  }

  DisposableEffect(webView, webViewDestroyed) {
    onDispose {
      webViewDestroyed.set(true)
      mainHandler.removeCallbacksAndMessages(null)
      (webView.parent as? ViewGroup)?.removeView(webView)
      webView.removeJavascriptInterface("AndroidBridge")
      webView.stopLoading()
      webView.destroy()
    }
  }

  val isActive = controller.activeToken != null
  val show = controller.isVisible
  val bounds = controller.boundsPx

  LaunchedEffect(pageReady, isActive, configJson) {
    if (pageReady && isActive) {
      safeEvaluateJs(
        webView = webView,
        destroyed = webViewDestroyed,
        script = "window.updateFridge(${JSONObject.quote(configJson)});"
      )
    }
  }

  LaunchedEffect(controller.resetNonce) {
    if (pageReady && show && controller.resetNonce > 0) {
      safeEvaluateJs(webView, webViewDestroyed, "window.resetView();")
    }
  }

  LaunchedEffect(controller.itemAddedPulseNonce) {
    if (pageReady && show && controller.itemAddedPulseNonce > 0) {
      safeEvaluateJs(webView, webViewDestroyed, "window.playItemAddedBounce();")
    }
  }

  LaunchedEffect(pageReady, show, controller.playWelcomeSpin) {
    if (pageReady && show && controller.playWelcomeSpin) {
      delay(250L)
      if (pageReady && controller.isVisible && !webViewDestroyed.get()) {
        safeEvaluateJs(webView, webViewDestroyed, "window.playWelcomeSpin();")
      }
      runCatching { controller.onWelcomeSpinConsumed() }
    }
  }

  LaunchedEffect(pageReady, show, bounds) {
    if (pageReady && show && bounds.isValid) {
      webView.post {
        if (pageReady && controller.isVisible) {
          safeEvaluateJs(webView, webViewDestroyed, "window.dispatchEvent(new Event('resize'));")
        }
      }
    }
  }

  key(recoveryKey) {
    val activeBounds = if (bounds.isValid) bounds else lastValidBounds
    val hasValidBounds = activeBounds.isValid
    val wDp = if (hasValidBounds) with(density) { activeBounds.width.toDp() } else 0.dp
    val hDp = if (hasValidBounds) with(density) { activeBounds.height.toDp() } else 0.dp
    val xOff = if (hasValidBounds) activeBounds.left else 0
    val yOff = if (hasValidBounds) activeBounds.top else 0
    val cornerRadius = controller.cornerRadius
    val cornerPx = with(density) { cornerRadius.toPx() }
    val cornerShape = if (cornerRadius > 0.dp) RoundedCornerShape(cornerRadius) else RectangleShape
    var appliedCornerPx by remember { mutableFloatStateOf(-1f) }
    var appliedSurfaceBg by remember { mutableStateOf("") }
    val surfaceBg = controller.surfaceBg

    AndroidView(
      factory = { webView },
      modifier = modifier
        .offset { IntOffset(xOff, yOff) }
        .size(wDp, hDp)
        .then(if (cornerRadius > 0.dp && show) Modifier.clip(cornerShape) else Modifier),
      update = { view ->
        val targetVisibility = if (show && bounds.isValid) View.VISIBLE else View.GONE
        if (view.visibility != targetVisibility) {
          view.visibility = targetVisibility
        }
        if (appliedSurfaceBg != surfaceBg) {
          appliedSurfaceBg = surfaceBg
          view.setBackgroundColor(parseSurfaceColorInt(surfaceBg))
        }
        if (appliedCornerPx != cornerPx) {
          appliedCornerPx = cornerPx
          if (cornerPx > 0f) {
            view.clipToOutline = true
            view.outlineProvider = object : ViewOutlineProvider() {
              override fun getOutline(v: View, outline: Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, cornerPx)
              }
            }
          } else {
            view.clipToOutline = false
            view.outlineProvider = ViewOutlineProvider.BACKGROUND
          }
        }
        if (show && bounds.isValid) {
          view.requestLayout()
        }
      }
    )
  }
}

/**
 * Lightweight placeholder slot placed in screens (`FridgeDesignerScreen`, `HomeScreen`) to measure
 * root-relative stage bounds and synchronize current screen props with [SharedFridge3DWebView].
 */
@Composable
fun Fridge3DStageSlot(
  colorKey: String,
  compartments: List<CompartmentEntity>,
  modifier: Modifier = Modifier,
  template: String = "top_freezer",
  openCompartmentId: String? = null,
  resetOrbitKey: Int = 0,
  itemAddedPulseKey: Int = 0,
  playWelcomeSpin: Boolean = false,
  onWelcomeSpinConsumed: () -> Unit = {},
  showStickers: Boolean = false,
  surfaceBg: String = DEFAULT_SURFACE_BG,
  cornerRadius: Dp = 0.dp,
  backdropColor: Color? = null,
  onDoorClick: ((compartmentId: String) -> Unit)? = null,
  onProjectionsUpdated: ((Map<String, DoorProjection>) -> Unit)? = null
) {
  val controller = LocalFridge3DStageController.current
  val slotToken = remember { Any() }

  val latestOnWelcomeConsumed by rememberUpdatedState(onWelcomeSpinConsumed)
  val latestOnDoorClick by rememberUpdatedState(onDoorClick)
  val latestOnProjectionsUpdated by rememberUpdatedState(onProjectionsUpdated)

  SideEffect {
    controller.updateSlot(
      token = slotToken,
      colorKey = colorKey,
      compartments = compartments,
      template = template,
      openCompartmentId = openCompartmentId,
      playWelcomeSpin = playWelcomeSpin,
      showStickers = showStickers,
      surfaceBg = surfaceBg,
      cornerRadius = cornerRadius,
      backdropColor = backdropColor,
      onWelcomeSpinConsumed = { latestOnWelcomeConsumed() },
      onDoorClick = latestOnDoorClick,
      onProjectionsUpdated = latestOnProjectionsUpdated
    )
  }

  LaunchedEffect(resetOrbitKey) {
    if (resetOrbitKey > 0) {
      controller.requestResetOrbit()
    }
  }

  LaunchedEffect(itemAddedPulseKey) {
    if (itemAddedPulseKey > 0) {
      controller.requestItemAddedBounce()
    }
  }

  DisposableEffect(controller, slotToken) {
    onDispose {
      controller.releaseSlot(slotToken)
    }
  }

  Box(
    modifier = modifier.onGloballyPositioned { coords ->
      if (!coords.isAttached || coords.size.width <= 0 || coords.size.height <= 0) {
        return@onGloballyPositioned
      }
      val root = controller.rootCoordinates
      val topLeft = if (root != null && root.isAttached) {
        root.localPositionOf(coords, Offset.Zero)
      } else {
        coords.positionInRoot()
      }
      controller.updateBounds(
        slotToken,
        StageBoundsPx(
          left = topLeft.x.roundToInt(),
          top = topLeft.y.roundToInt(),
          width = coords.size.width,
          height = coords.size.height
        )
      )
    }
  )
}

private fun safeEvaluateJs(
  webView: WebView,
  destroyed: AtomicBoolean,
  script: String
) {
  if (destroyed.get() || !webView.isAttachedToWindow) return
  webView.evaluateJavascript(script, null)
}

private fun parseSurfaceColorInt(hex: String): Int =
  runCatching { AndroidColor.parseColor(hex) }.getOrDefault(0xFFFAF8F5.toInt())

private fun buildConfigJson(
  template: String,
  colorKey: String,
  compartments: List<CompartmentEntity>,
  openCompartmentId: String?,
  showStickers: Boolean,
  surfaceBg: String
): String {
  val compsArray = JSONArray()
  compartments.forEach { c ->
    compsArray.put(
      JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("kind", c.kind.name)
        put("x", c.x.toDouble())
        put("y", c.y.toDouble())
        put("w", c.w.toDouble())
        put("h", c.h.toDouble())
        put("shelves", c.shelves)
      }
    )
  }
  val showDotGrid = showStickers || !surfaceBg.equals("#FFFFFF", ignoreCase = true)
  return JSONObject().apply {
    put("template", template)
    put("colorKey", colorKey)
    put("openCompartmentId", openCompartmentId ?: JSONObject.NULL)
    put("showStickers", showStickers)
    put("springReturn", showStickers)
    put("surfaceBg", surfaceBg)
    put("showDotGrid", showDotGrid)
    put("compartments", compsArray)
  }.toString()
}

private fun parseProjections(json: String): Map<String, DoorProjection> {
  val out = HashMap<String, DoorProjection>()
  runCatching {
    val arr = JSONArray(json)
    for (i in 0 until arr.length()) {
      val obj = arr.getJSONObject(i)
      val id = obj.optString("id")
      if (id.isNotEmpty()) {
        out[id] = DoorProjection(
          xFrac = obj.optDouble("x", 0.5).toFloat(),
          yFrac = obj.optDouble("y", 0.5).toFloat(),
          visible = obj.optBoolean("visible", false)
        )
      }
    }
  }
  return out
}
