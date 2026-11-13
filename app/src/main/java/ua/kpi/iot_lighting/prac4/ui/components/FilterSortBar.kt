package ua.kpi.iot_lighting.prac4.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ua.kpi.iot_lighting.prac4.domain.model.DeviceSortOption
import ua.kpi.iot_lighting.prac4.domain.model.FilterSortState
import ua.kpi.iot_lighting.prac4.domain.model.StatusFilter
import ua.kpi.iot_lighting.prac4.ui.theme.AmberContainer
import ua.kpi.iot_lighting.prac4.ui.theme.AmberPrimary
import ua.kpi.iot_lighting.prac4.ui.theme.AzureAccent
import ua.kpi.iot_lighting.prac4.ui.theme.AzureContainer
import ua.kpi.iot_lighting.prac4.ui.theme.OutlineSubtle
import ua.kpi.iot_lighting.prac4.ui.theme.SurfaceCard
import ua.kpi.iot_lighting.prac4.ui.theme.SurfaceElevated
import ua.kpi.iot_lighting.prac4.ui.theme.TextSecondary

@Composable
fun FilterSortBar(
    filterSort: FilterSortState,
    availableZones: List<String>,
    filteredCount: Int,
    totalCount: Int,
    maxDevicePowerW: Int,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onZoneSelected: (String?) -> Unit,
    onMinPowerChange: (Int) -> Unit,
    onSortOptionChange: (DeviceSortOption) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, OutlineSubtle),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            OutlinedTextField(
                value = filterSort.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Пошук за назвою, вулицею або зоною (укр.)...",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    if (filterSort.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Очистити пошук",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AzureAccent,
                    unfocusedBorderColor = OutlineSubtle,
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated,
                ),
                shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    FilterChip(
                        selected = filtersExpanded || filterSort.activeFilterCount > 0,
                        onClick = { filtersExpanded = !filtersExpanded },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        label = {
                            val suffix = if (filterSort.activeFilterCount > 0) {
                                " (${filterSort.activeFilterCount})"
                            } else {
                                ""
                            }
                            Text(
                                text = "Фільтри$suffix",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AzureContainer,
                            selectedLabelColor = AzureAccent,
                            selectedLeadingIconColor = AzureAccent,
                        ),
                    )

                    FilterChip(
                        selected = filterSort.sortOption != DeviceSortOption.BY_NAME,
                        onClick = {
                            val entries = DeviceSortOption.entries
                            val next = entries[(filterSort.sortOption.ordinal + 1) % entries.size]
                            onSortOptionChange(next)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        label = {
                            Text(
                                text = filterSort.sortOption.label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberContainer,
                            selectedLabelColor = AmberPrimary,
                            selectedLeadingIconColor = AmberPrimary,
                        ),
                    )
                }

                if (filterSort.isDefault) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$filteredCount з $totalCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            if (!filterSort.isDefault) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Показано $filteredCount з $totalCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        softWrap = false,
                    )
                    TextButton(onClick = onResetFilters) {
                        Text(
                            text = "Скинути",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }

            AnimatedVisibility(visible = filtersExpanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .heightIn(max = 210.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = "Стан живлення",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusFilter.entries.forEach { status ->
                            FilterChip(
                                selected = filterSort.statusFilter == status,
                                onClick = { onStatusFilterChange(status) },
                                label = {
                                    Text(status.label, style = MaterialTheme.typography.labelMedium)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberContainer,
                                    selectedLabelColor = AmberPrimary,
                                ),
                            )
                        }
                    }

                    if (availableZones.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Зони",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = filterSort.selectedZone == null,
                                    onClick = { onZoneSelected(null) },
                                    label = { Text("Усі зони", style = MaterialTheme.typography.labelMedium) },
                                )
                            }
                            items(availableZones, key = { it }) { zone ->
                                FilterChip(
                                    selected = filterSort.selectedZone == zone,
                                    onClick = { onZoneSelected(zone) },
                                    label = { Text(zone, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AzureContainer,
                                        selectedLabelColor = AzureAccent,
                                    ),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Мінімальна потужність: ${filterSort.minPowerW} Вт",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        Text(
                            text = "до $maxDevicePowerW Вт",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    Slider(
                        value = filterSort.minPowerW.toFloat().coerceIn(0f, maxDevicePowerW.toFloat()),
                        onValueChange = { raw ->
                            val rounded = (raw / 10f).roundToInt() * 10
                            onMinPowerChange(rounded)
                        },
                        valueRange = 0f..maxDevicePowerW.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = AmberPrimary,
                            activeTrackColor = AmberPrimary,
                        ),
                    )

                    Text(
                        text = "Сортування",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(DeviceSortOption.entries) { option ->
                            FilterChip(
                                selected = filterSort.sortOption == option,
                                onClick = { onSortOptionChange(option) },
                                label = {
                                    Text(option.label, style = MaterialTheme.typography.labelMedium)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AzureContainer,
                                    selectedLabelColor = AzureAccent,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
