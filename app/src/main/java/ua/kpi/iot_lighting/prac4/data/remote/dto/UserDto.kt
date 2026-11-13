package ua.kpi.iot_lighting.prac4.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
)

data class LoginResponseDto(
    @SerializedName("token") val token: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
)
