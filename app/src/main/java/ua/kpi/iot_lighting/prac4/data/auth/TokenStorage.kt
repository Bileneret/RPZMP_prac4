package ua.kpi.iot_lighting.prac4.data.auth

interface TokenStorage {
    fun read(): String?
    fun write(token: String?)
    fun clear()
}
