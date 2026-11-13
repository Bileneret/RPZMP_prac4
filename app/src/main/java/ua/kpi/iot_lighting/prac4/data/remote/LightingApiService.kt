package ua.kpi.iot_lighting.prac4.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import ua.kpi.iot_lighting.prac4.data.remote.dto.CreateLightRequestDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.FaultModeDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.LightDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.LoginRequestDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.LoginResponseDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.PowerUpdateRequestDto

interface LightingApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    @GET("lights")
    suspend fun getLights(): List<LightDto>

    @POST("lights")
    suspend fun createLight(@Body request: CreateLightRequestDto): LightDto

    @PUT("lights/{id}/power")
    suspend fun setPower(
        @Path("id") id: Long,
        @Body request: PowerUpdateRequestDto,
    ): LightDto

    @DELETE("lights/{id}")
    suspend fun deleteLight(@Path("id") id: Long)

    @GET("debug/fault")
    suspend fun getFaultMode(): FaultModeDto

    @POST("debug/fault")
    suspend fun setFaultMode(@Body request: FaultModeDto): FaultModeDto
}
