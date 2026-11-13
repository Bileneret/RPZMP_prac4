package ua.kpi.iot_lighting.prac4.domain.model

enum class StatusFilter(val label: String) {
    ALL("Усі"),
    ON("Увімкнено"),
    OFF("Вимкнено"),
}

enum class DeviceSortOption(val label: String) {
    BY_NAME("Назва (А–Я)"),
    BY_POWER_DESC("Потужність ↓"),
    BY_LAST_UPDATED("Спочатку нові"),
}

data class FilterSortState(
    val searchQuery: String = "",
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val selectedZone: String? = null,
    val minPowerW: Int = 0,
    val sortOption: DeviceSortOption = DeviceSortOption.BY_NAME,
) {
    val activeFilterCount: Int
        get() = listOf(
            searchQuery.isNotBlank(),
            statusFilter != StatusFilter.ALL,
            selectedZone != null,
            minPowerW > 0,
        ).count { it }

    val isDefault: Boolean
        get() = activeFilterCount == 0 && sortOption == DeviceSortOption.BY_NAME
}

object DeviceFilterSortLogic {
    fun apply(
        devices: List<LightDevice>,
        filter: FilterSortState,
    ): List<LightDevice> {
        val query = filter.searchQuery.trim().lowercase()
        val filtered = devices.filter { device ->
            val matchesQuery = query.isEmpty() ||
                device.name.lowercase().contains(query) ||
                device.zone.lowercase().contains(query)

            val matchesStatus = when (filter.statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.ON -> device.isPoweredOn
                StatusFilter.OFF -> !device.isPoweredOn
            }

            val matchesZone = filter.selectedZone == null ||
                device.zone.equals(filter.selectedZone, ignoreCase = true)

            val matchesPower = device.powerW >= filter.minPowerW

            matchesQuery && matchesStatus && matchesZone && matchesPower
        }

        return when (filter.sortOption) {
            DeviceSortOption.BY_NAME -> filtered.sortedWith(
                compareBy<LightDevice> { it.name.lowercase() }.thenBy { it.id },
            )
            DeviceSortOption.BY_POWER_DESC -> filtered.sortedWith(
                compareByDescending<LightDevice> { it.powerW }
                    .thenBy { it.name.lowercase() }
                    .thenBy { it.id },
            )
            DeviceSortOption.BY_LAST_UPDATED -> filtered.sortedWith(
                compareByDescending<LightDevice> { it.lastUpdatedAt }
                    .thenByDescending { it.id },
            )
        }
    }
}
