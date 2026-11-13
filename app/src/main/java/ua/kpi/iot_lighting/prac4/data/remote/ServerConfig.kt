package ua.kpi.iot_lighting.prac4.data.remote

object ServerConfig {
    const val MOCK_SERVER_PORT = 8081

    /**
     * За замовчуванням використовується loopback із adb reverse (tcp:8081 -> tcp:8081),
     * що працює як на фізичному смартфоні (Wi-Fi/USB ADB), так і на емуляторі з adb reverse.
     */
    const val BASE_URL = "http://127.0.0.1:$MOCK_SERVER_PORT/"
    const val EMULATOR_BASE_URL = "http://10.0.2.2:$MOCK_SERVER_PORT/"

    const val WEATHER_BASE_URL = "https://api.open-meteo.com/"
    const val DEFAULT_CITY = "Кривий Ріг"
    const val DEFAULT_LAT = 47.9635
    const val DEFAULT_LNG = 33.4325
}
