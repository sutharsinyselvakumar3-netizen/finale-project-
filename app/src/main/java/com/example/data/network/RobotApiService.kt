package com.example.data.network

import com.example.data.network.dto.ActuatorTimedRequestDto
import com.example.data.network.dto.AiDetectionResultDto
import com.example.data.network.dto.ArmPositionRequestDto
import com.example.data.network.dto.AutoRequestDto
import com.example.data.network.dto.EstopRequestDto
import com.example.data.network.dto.ModeRequestDto
import com.example.data.network.dto.MoveRequestDto
import com.example.data.network.dto.PanTiltRequestDto
import com.example.data.network.dto.RelayRequestDto
import com.example.data.network.dto.RobotStatusResponseDto
import com.example.data.network.dto.SimpleApiResponseDto
import com.example.data.network.dto.SpeedRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface RobotApiService {

    @GET("api/status")
    suspend fun getStatus(): Response<RobotStatusResponseDto>

    @GET("api/sensors")
    suspend fun getSensors(): Response<RobotStatusResponseDto>

    @POST("api/mode")
    suspend fun setMode(@Body request: ModeRequestDto): Response<SimpleApiResponseDto>

    @POST("api/move")
    suspend fun sendMove(@Body request: MoveRequestDto): Response<SimpleApiResponseDto>

    @POST("api/speed")
    suspend fun setSpeed(@Body request: SpeedRequestDto): Response<SimpleApiResponseDto>

    @POST("api/camera/pantilt")
    suspend fun setPanTilt(@Body request: PanTiltRequestDto): Response<SimpleApiResponseDto>

    @POST("api/actuators/relay")
    suspend fun setRelay(@Body request: RelayRequestDto): Response<SimpleApiResponseDto>

    @POST("api/estop")
    suspend fun triggerEstop(@Body request: EstopRequestDto): Response<SimpleApiResponseDto>

    @POST("api/auto")
    suspend fun controlAuto(@Body request: AutoRequestDto): Response<SimpleApiResponseDto>

    @POST("api/ai/target")
    suspend fun sendWeedTarget(@Body request: AiDetectionResultDto): Response<SimpleApiResponseDto>

    @POST("api/actuators/arm")
    suspend fun moveArm(@Body request: ArmPositionRequestDto): Response<SimpleApiResponseDto>

    @POST("api/actuators/cutter")
    suspend fun controlCutter(@Body request: ActuatorTimedRequestDto): Response<SimpleApiResponseDto>

    @POST("api/actuators/pump/chemical")
    suspend fun controlChemicalPump(@Body request: ActuatorTimedRequestDto): Response<SimpleApiResponseDto>

    @POST("api/actuators/pump/water")
    suspend fun controlWaterPump(@Body request: ActuatorTimedRequestDto): Response<SimpleApiResponseDto>
}
