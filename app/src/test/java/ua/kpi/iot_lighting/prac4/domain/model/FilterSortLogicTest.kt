package ua.kpi.iot_lighting.prac4.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterSortLogicTest {
    private val devices = listOf(
        LightDevice(1L, "Альфа ліхтар", "Вулиця", 120, true, lastUpdatedAt = 100L),
        LightDevice(2L, "Бета Склад", "Склад", 200, false, lastUpdatedAt = 300L),
        LightDevice(3L, "Гамма Парк", "Парк", 60, true, lastUpdatedAt = 200L),
        LightDevice(4L, "Дельта Парковка", "Паркування", 90, false, lastUpdatedAt = 50L),
    )

    @Test
    fun `search query matches name or zone case-insensitively`() {
        val byName = DeviceFilterSortLogic.apply(devices, FilterSortState(searchQuery = "альфа"))
        assertEquals(listOf(1L), byName.map { it.id })

        val byZone = DeviceFilterSortLogic.apply(devices, FilterSortState(searchQuery = "парк"))
        assertEquals(listOf(3L, 4L), byZone.map { it.id })
    }

    @Test
    fun `multi-criteria filtering combines status zone and minPower`() {
        val filter = FilterSortState(
            statusFilter = StatusFilter.ON,
            selectedZone = "Вулиця",
            minPowerW = 100,
        )
        val result = DeviceFilterSortLogic.apply(devices, filter)
        assertEquals(listOf(1L), result.map { it.id })
        assertEquals(3, filter.activeFilterCount)
        assertFalse(filter.isDefault)
    }

    @Test
    fun `sort options order by name power desc and lastUpdated desc`() {
        val byPower = DeviceFilterSortLogic.apply(
            devices,
            FilterSortState(sortOption = DeviceSortOption.BY_POWER_DESC),
        )
        assertEquals(listOf(2L, 1L, 4L, 3L), byPower.map { it.id })

        val byUpdated = DeviceFilterSortLogic.apply(
            devices,
            FilterSortState(sortOption = DeviceSortOption.BY_LAST_UPDATED),
        )
        assertEquals(listOf(2L, 3L, 1L, 4L), byUpdated.map { it.id })
        assertEquals(0, FilterSortState(sortOption = DeviceSortOption.BY_POWER_DESC).activeFilterCount)
        assertFalse(FilterSortState(sortOption = DeviceSortOption.BY_POWER_DESC).isDefault)
        assertTrue(FilterSortState().isDefault)
    }
}
