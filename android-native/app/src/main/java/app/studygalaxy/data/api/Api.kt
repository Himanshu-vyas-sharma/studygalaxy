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
    @POST("/auth/attach-phone") suspend fun attachPhone(@Body req: AttachPhoneReq): SimpleOk
    @POST("/auth/claim-name")   suspend fun claimName(@Body req: ClaimNameReq): ClaimNameReply
    @GET("/auth/me")            suspend fun me(): User

    @GET("/galaxy")                                   suspend fun galaxy(): Galaxy
    @GET("/subjects")                                 suspend fun subjects(): List<Planet>
    @POST("/subjects")          suspend fun createSubject(@Body req: CreateSubjectReq): Planet
    @POST("/sessions/start")    suspend fun startSession(@Body req: StartSessionWithTodosReq): StartSessionWithTodosReply
    @POST("/sessions/complete") suspend fun completeSession(@Body req: CompleteSessionWithTodosReq): CompleteSessionReply
    @POST("/sessions/break")    suspend fun breakSession(@Body req: BreakSessionReq): SimpleOk
    @GET("/sessions/{id}/todos") suspend fun sessionTodos(@Path("id") id: String): List<Todo>

    @GET("/notes")                                    suspend fun notes(): List<Note>
    @POST("/notes")             suspend fun createNote(@Body req: CreateNoteReq): Note
    @retrofit2.http.PUT("/notes/{id}") suspend fun updateNote(@Path("id") id: String, @Body req: UpdateNoteReq): SimpleOk
    @retrofit2.http.DELETE("/notes/{id}") suspend fun deleteNote(@Path("id") id: String): SimpleOk

    @GET("/circles")                                  suspend fun circles(): List<Circle>
    @POST("/circles")           suspend fun createCircle(@Body req: CreateCircleReq): Circle
    @POST("/circles/join")      suspend fun joinCircle(@Body req: JoinCircleReq): Circle
    @GET("/circles/{id}/presence") suspend fun circlePresence(@Path("id") id: String): PresenceReply
    @GET("/circles/friends-focusing") suspend fun friendsFocusing(): FriendsFocusingReply

    @GET("/stats/weekly")                             suspend fun statsWeekly(): WeeklyStats

    @POST("/profile")           suspend fun saveProfile(@Body req: ProfileReq): SimpleOk
}
