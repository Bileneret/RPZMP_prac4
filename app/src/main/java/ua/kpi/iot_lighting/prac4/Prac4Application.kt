package ua.kpi.iot_lighting.prac4

import android.app.Application
import ua.kpi.iot_lighting.prac4.di.AppContainer

class Prac4Application : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
