package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.EqualizerState
import com.example.ui.theme.DarkPurpleBorder
import com.example.ui.theme.DarkPurpleCard
import com.example.ui.theme.DarkPurpleSurface
import com.example.ui.theme.ElectricPink
import com.example.ui.theme.NeonFuchsia
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SoftPurple

@Composable
fun EqualizerDialog(
    equalizerState: EqualizerState,
    onToggleEnabled: (Boolean) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onBandLevelChange: (Int, Int) -> Unit,
    onSelectPreset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, DarkPurpleBorder, RoundedCornerShape(24.dp))
                .testTag("equalizer_dialog"),
            colors = CardDefaults.cardColors(containerColor = DarkPurpleSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(NeonPurple, NeonFuchsia))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Audio Equalizer",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Fine-tune frequencies & bass boost",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = SoftPurple.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Power Toggle Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkPurpleCard)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Equalizer Power",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (equalizerState.isEnabled) "Enhanced DSP Active" else "Bypassed",
                            color = if (equalizerState.isEnabled) NeonPurple else Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = equalizerState.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonPurple,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = DarkPurpleBorder
                        ),
                        modifier = Modifier.testTag("equalizer_power_switch")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bass Boost Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkPurpleCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkPurpleBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = ElectricPink,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Deep Bass Boost",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "${(equalizerState.bassBoostStrength / 10)}%",
                                fontWeight = FontWeight.Bold,
                                color = ElectricPink,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Slider(
                            value = equalizerState.bassBoostStrength.toFloat(),
                            onValueChange = { onBassBoostChange(it.toInt()) },
                            valueRange = 0f..1000f,
                            enabled = equalizerState.isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricPink,
                                activeTrackColor = ElectricPink,
                                inactiveTrackColor = DarkPurpleBorder
                            ),
                            modifier = Modifier.testTag("bass_boost_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Presets Chips
                Text(
                    text = "Presets",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(equalizerState.presets) { preset ->
                        val isSelected = equalizerState.currentPreset.equals(preset, ignoreCase = true)
                        val bgModifier = if (isSelected) {
                            Modifier.background(Brush.horizontalGradient(listOf(NeonPurple, NeonFuchsia)))
                        } else {
                            Modifier.background(DarkPurpleCard)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .then(bgModifier)
                                .border(
                                    1.dp,
                                    if (isSelected) Color.Transparent else DarkPurpleBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable(enabled = equalizerState.isEnabled) {
                                    onSelectPreset(preset)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("preset_$preset")
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else SoftPurple
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Frequency Bands Sliders
                Text(
                    text = "Frequency Bands",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                equalizerState.bands.forEach { band ->
                    val freqLabel = when {
                        band.centerFreqHz >= 1000 -> "${band.centerFreqHz / 1000} kHz"
                        else -> "${band.centerFreqHz} Hz"
                    }
                    val dB = band.currentLevel / 100

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = freqLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SoftPurple,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.width(64.dp)
                        )

                        Slider(
                            value = band.currentLevel.toFloat(),
                            onValueChange = { onBandLevelChange(band.id, it.toInt()) },
                            valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                            enabled = equalizerState.isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonPurple,
                                activeTrackColor = NeonPurple,
                                inactiveTrackColor = DarkPurpleBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("band_slider_${band.id}")
                        )

                        Text(
                            text = "${if (dB > 0) "+$dB" else "$dB"} dB",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (dB > 0) NeonPurple else Color.Gray,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.width(48.dp)
                        )
                    }
                }
            }
        }
    }
}
