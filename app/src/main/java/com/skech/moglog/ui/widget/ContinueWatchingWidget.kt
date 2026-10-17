package com.skech.moglog.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.skech.moglog.MainActivity
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.domain.repository.DramaRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

private val DramaIdParamKey = ActionParameters.Key<Int>("drama_id")

class ContinueWatchingWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun dramaRepository(): DramaRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val watching = try {
            val entry = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java
            )
            entry.dramaRepository().getAllDramas().first()
                .filter { !it.isDeleted && it.status == "Watching" }
                .sortedByDescending { it.updatedAt }
                .take(2)
        } catch (_: Exception) {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                WidgetContent(watching = watching)
            }
        }
    }

    @Composable
    private fun WidgetContent(watching: List<Drama>) {
        val bg = ColorProvider(Color(0xFF1C1C1E))
        val cardBg = ColorProvider(Color(0xFF2C2C2E))
        val accent = ColorProvider(Color(0xFF30D158))
        val textPrimary = ColorProvider(Color(0xFFFFFFFF))
        val textSecondary = ColorProvider(Color(0x99EBEBF5))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(bg)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONTINUE WATCHING",
                    style = TextStyle(
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (watching.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "No active shows. Tap to open Moglog.",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 13.sp
                        )
                    )
                }
            } else {
                watching.forEachIndexed { index, drama ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(12.dp)
                            .background(cardBg)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = drama.title,
                                maxLines = 1,
                                style = TextStyle(
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            val totalText = if (drama.totalEpisodes > 0) " / ${drama.totalEpisodes}" else ""
                            Text(
                                text = "S${drama.currentSeason.coerceAtLeast(1)} · E${drama.episodesWatched}$totalText",
                                maxLines = 1,
                                style = TextStyle(
                                    color = textSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        Box(
                            modifier = GlanceModifier
                                .cornerRadius(999.dp)
                                .background(accent)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .clickable(
                                    actionRunCallback<IncrementEpisodeAction>(
                                        actionParametersOf(DramaIdParamKey to drama.id)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+1 EP",
                                style = TextStyle(
                                    color = ColorProvider(Color.Black),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    if (index < watching.lastIndex) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }
}

class IncrementEpisodeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val dramaId = parameters[DramaIdParamKey] ?: return
        try {
            val entry = EntryPointAccessors.fromApplication(
                context.applicationContext,
                ContinueWatchingWidget.WidgetEntryPoint::class.java
            )
            val repo = entry.dramaRepository()
            val existing = repo.getLocalDramaById(dramaId) ?: return
            val nextEp = if (existing.totalEpisodes > 0) {
                (existing.episodesWatched + 1).coerceAtMost(existing.totalEpisodes)
            } else {
                existing.episodesWatched + 1
            }
            val nextStatus = if (existing.totalEpisodes > 0 && nextEp >= existing.totalEpisodes) {
                "Done"
            } else {
                "Watching"
            }
            val now = System.currentTimeMillis()
            repo.saveDrama(
                existing.copy(
                    episodesWatched = nextEp,
                    status = nextStatus,
                    updatedAt = now,
                    completedAt = if (nextStatus == "Done" && existing.status != "Done") now else existing.completedAt
                )
            )
            ContinueWatchingWidget().updateAll(context)
        } catch (_: Exception) {
        }
    }
}

class ContinueWatchingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ContinueWatchingWidget()
}
