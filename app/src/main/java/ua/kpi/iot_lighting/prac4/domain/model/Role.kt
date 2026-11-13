package ua.kpi.iot_lighting.prac4.domain.model

enum class Role(
    val wireName: String,
    val displayName: String,
    val canManageCatalog: Boolean,
    val canTogglePower: Boolean,
) {
    USER(
        wireName = "user",
        displayName = "Оператор",
        canManageCatalog = false,
        canTogglePower = true,
    ),
    ADMIN(
        wireName = "admin",
        displayName = "Адміністратор",
        canManageCatalog = true,
        canTogglePower = true,
    );

    companion object {
        fun fromWire(value: String?): Role {
            return entries.firstOrNull { it.wireName.equals(value, ignoreCase = true) } ?: USER
        }
    }
}
