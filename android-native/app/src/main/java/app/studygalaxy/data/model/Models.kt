package app.studygalaxy.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val cosmicName: String,
    val avatarSeed: String,
    val level: Int,
    val totalFocusMin: Int,
    val streakDays: Int,
    val phoneNumber: String? = null
)

@Serializable data class AuthReply(val jwt: String, val user: User)

@Serializable data class AnonymousReq(val deviceId: String)
@Serializable data class SignupReq(val phoneNumber: String, val password: String, val deviceId: String, val cosmicName: String? = null)
@Serializable data class LoginReq(val phoneNumber: String, val password: String, val deviceId: String)
@Serializable data class ClaimNameReq(val name: String)
@Serializable data class ClaimNameReply(val ok: Boolean, val cosmicName: String)

@Serializable data class Planet(val id: String, val name: String, val color: String, val totalMin: Int)
@Serializable data class Star(val id: String, val subjectId: String? = null, val min: Int, val at: Long, val rating: Int)
@Serializable data class BlackHole(val id: String, val subjectId: String? = null, val at: Long)
@Serializable data class Galaxy(val planets: List<Planet>, val stars: List<Star>, val blackHoles: List<BlackHole>)

@Serializable data class StartSessionReq(val subjectId: String? = null, val plannedMin: Int, val intention: String? = null)
@Serializable data class StartSessionReply(val sessionId: String, val serverStartTs: Long)
@Serializable data class CompleteSessionReq(val sessionId: String, val selfRating: Int? = null)
@Serializable data class CompleteSessionReply(val ok: Boolean, val actualMin: Int)
@Serializable data class BreakSessionReq(val sessionId: String)
@Serializable data class SimpleOk(val ok: Boolean)

@Serializable data class Circle(val id: String, val name: String, val inviteCode: String? = null)
@Serializable data class CreateCircleReq(val name: String)
@Serializable data class JoinCircleReq(val inviteCode: String)

@Serializable data class HeatmapDay(val date: String, val min: Int)
@Serializable data class WeeklyStats(
    val heatmap: List<HeatmapDay>,
    val weekTotalMin: Int,
    val streakDays: Int,
    val totalFocusMin: Int,
    val level: Int
)

@Serializable data class ProfileReq(
    val examGoal: String? = null,
    val dailyHours: Int? = null,
    val topSubjects: List<String>? = null,
    val studyTime: String? = null,
    val biggestDistraction: String? = null
)
