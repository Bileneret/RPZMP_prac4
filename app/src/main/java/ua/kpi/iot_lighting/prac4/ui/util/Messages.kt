package ua.kpi.iot_lighting.prac4.ui.util

object Messages {
    val DEMO_ZONES = listOf("Вулиця", "Паркування", "Фасад", "Парк", "Склад")

    val FAULT_MODES = listOf(
        "none" to "Штатний режим (200 OK)",
        "slow" to "Повільна мережа (+1.8 с)",
        "500" to "Збій сервера (HTTP 500)",
        "429" to "Ліміт запитів (HTTP 429)",
        "401" to "Прострочений токен (HTTP 401)",
    )
}
