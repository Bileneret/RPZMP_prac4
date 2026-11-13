package ua.kpi.iot_lighting.prac4.data.source

import ua.kpi.iot_lighting.prac4.data.remote.LightingApiService
import ua.kpi.iot_lighting.prac4.data.remote.dto.CreateLightRequestDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.FaultModeDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.LightDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.PowerUpdateRequestDto

class RetrofitLightingRemoteDataSource(
    private val api: LightingApiService,
) : LightingRemoteDataSource {
    override suspend fun getLights(): List<LightDto> = api.getLights()

    override suspend fun createLight(
        name: String,
        zone: String,
        powerW: Int,
        latitude: Double,
        longitude: Double,
    ): LightDto {
        return api.createLight(
            CreateLightRequestDto(
                name = name,
                zone = zone,
                powerW = powerW,
                latitude = latitude,
                longitude = longitude,
            ),
        )
    }

    override suspend fun setPower(id: Long, poweredOn: Boolean): LightDto {
        return api.setPower(id = id, request = PowerUpdateRequestDto(isPoweredOn = poweredOn))
    }

    override suspend fun deleteLight(id: Long) {
        api.deleteLight(id)
    }

    override suspend fun getFaultMode(): String = api.getFaultMode().mode

    override suspend fun setFaultMode(mode: String) {
        api.setFaultMode(FaultModeDto(mode = mode))
    }
}
