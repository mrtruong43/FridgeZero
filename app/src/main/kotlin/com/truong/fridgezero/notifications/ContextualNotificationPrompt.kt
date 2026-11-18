package com.truong.fridgezero.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truong.fridgezero.R
import com.truong.fridgezero.data.AppConfig
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.PaperCream
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite
import kotlinx.coroutines.launch

@Stable
class ReminderPromptController(
  val remindersEnabled: Boolean,
  val onItemSavedWithExpiry: (daysLeft: Int) -> Unit,
  val onToggleReminders: () -> Unit
)

/**
 * Contextual POST_NOTIFICATIONS controller (spec §0 & §4.2):
 * Never prompts on cold app launch; prompts only when saving a short-lived item or toggling reminders.
 */
@Composable
fun rememberReminderPromptController(appConfig: AppConfig): ReminderPromptController {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val remindersEnabled by appConfig.expiryRemindersEnabled
    .collectAsStateWithLifecycle(initialValue = false)
  val promptShown by appConfig.notificationPromptShown
    .collectAsStateWithLifecycle(initialValue = false)

  var showDialog by remember { mutableStateOf(false) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    scope.launch {
      appConfig.setNotificationPromptShown(true)
      if (granted) {
        appConfig.setExpiryRemindersEnabled(true)
        ExpiryReminderScheduler.scheduleDaily(context)
      } else {
        appConfig.setExpiryRemindersEnabled(false)
      }
    }
  }

  fun enableWithContextualPermission() {
    if (hasNotificationPermission(context)) {
      scope.launch {
        appConfig.setExpiryRemindersEnabled(true)
        appConfig.setNotificationPromptShown(true)
      }
      ExpiryReminderScheduler.scheduleDaily(context)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  if (showDialog) {
    Dialog(onDismissRequest = {
      showDialog = false
      scope.launch { appConfig.setNotificationPromptShown(true) }
    }) {
      NeoBrutalistCard(containerColor = PaperCream, modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          StickerBadge(text = "🔔 NHẮC HẠN", containerColor = LemonWarn)
          Text(
            text = stringResource(R.string.reminder_prompt_title),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = InkBlack
          )
          Text(
            text = stringResource(R.string.reminder_prompt_body),
            fontFamily = BodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = InkBlack
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            NeoBrutalistButton(
              onClick = {
                showDialog = false
                scope.launch { appConfig.setNotificationPromptShown(true) }
              },
              modifier = Modifier.weight(1f),
              containerColor = SurfaceWhite
            ) {
              Text(
                text = stringResource(R.string.reminder_prompt_later),
                fontFamily = BodyFont,
                fontWeight = FontWeight.Bold,
                color = InkBlack
              )
            }
            NeoBrutalistButton(
              onClick = {
                showDialog = false
                enableWithContextualPermission()
              },
              modifier = Modifier.weight(1f),
              containerColor = LimeFresh
            ) {
              Text(
                text = stringResource(R.string.reminder_prompt_confirm),
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                color = InkBlack
              )
            }
          }
        }
      }
    }
  }

  return remember(remindersEnabled, promptShown) {
    ReminderPromptController(
      remindersEnabled = remindersEnabled && hasNotificationPermission(context),
      onItemSavedWithExpiry = { daysLeft ->
        if (daysLeft <= 7 && (!remindersEnabled || !hasNotificationPermission(context)) && !promptShown) {
          showDialog = true
        } else if (remindersEnabled && hasNotificationPermission(context)) {
          ExpiryReminderScheduler.scheduleDaily(context)
        }
      },
      onToggleReminders = {
        if (remindersEnabled && hasNotificationPermission(context)) {
          scope.launch { appConfig.setExpiryRemindersEnabled(false) }
          ExpiryReminderScheduler.cancel(context)
        } else if (hasNotificationPermission(context)) {
          enableWithContextualPermission()
        } else {
          showDialog = true
        }
      }
    )
  }
}

private fun hasNotificationPermission(context: Context): Boolean {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
  return ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.POST_NOTIFICATIONS
  ) == PackageManager.PERMISSION_GRANTED
}
