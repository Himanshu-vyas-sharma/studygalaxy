package app.studygalaxy.data.api

import app.studygalaxy.data.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface Api {
    @POST("/auth/anonymous")    suspend fun anonymous(@Body req: AnonymousReq): AuthReply
    @POST("/auth/signup")       suspend fun signup(@Body req: SignupReq): AuthReply
    @POST("/auth/login")        suspend fun login(@Body req: LoginReq): AuthReply
    @POST("/auth/claim-name")   suspend fun claimName(@Body req: ClaimNameReq): ClaimNameReply
    @GET("/auth/me")            suspend fun me(): User

    @GET("/galaxy")                                   suspend fun galaxy(): Galaxy
    @POST("/sessions/start")    suspend fun startSession(@Body req: StartSessionReq): StartSessionReply
    @POST("/sessions/complete") suspend fun completeSession(@Body req: CompleteSessionReq): CompleteSessionReply
    @POST("/sessions/break")    suspend fun breakSession(@Body req: BreakSessionReq): SimpleOk

    @GET("/circles")                                  suspend fun circles(): List<Circle>
    @POST("/circles")           suspend fun createCircle(@Body req: CreateCircleReq): Circle
    @POST("/circles/join")      suspend fun joinCircle(@Body req: JoinCircleReq): Circle

    @GET("/stats/weekly")                             suspend fun statsWeekly(): WeeklyStats

    @POST("/profile")           suspend fun saveProfile(@Body req: ProfileReq): SimpleOk
}
