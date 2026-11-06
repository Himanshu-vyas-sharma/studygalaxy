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
@Serializable data class AttachPhoneReq(val phoneNumber: String, val password: String)

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

@Serializable data class Circle(
    val id: String,
    val name: String,
    val inviteCode: String? = null,
    val memberCount: Int = 0,
    val focusing: List<String> = emptyList()
)

/** One member of a circle who is in a live focus session right now. */
@Serializable data class FocusingMember(
    val userId: String,
    val cosmicName: String,
    val avatarSeed: String? = null,
    val startedAt: Long = 0L,
    val plannedMin: Int = 0,
    val endsAt: Long = 0L,
    val intention: String? = null
)

/** A `presence` frame pushed over the circle socket. */
@Serializable data class PresenceFrame(
    val type: String = "presence",
    val circleId: String? = null,
    val online: List<String> = emptyList(),
    val focusing: List<FocusingMember> = emptyList()
)

/** Any non-presence frame: session_start, session_complete, session_broken. */
@Serializable data class CircleEventFrame(
    val type: String = "event",
    val cosmicName: String? = null,
    val at: Long = 0L,
    val actualMin: Int? = null,
    val plannedMin: Int? = null,
    val intention: String? = null
)

@Serializable data class PresenceReply(val focusing: List<FocusingMember> = emptyList())

@Serializable data class FriendFocusing(
    val userId: String,
    val cosmicName: String,
    val circleName: String? = null,
    val endsAt: Long = 0L,
    val intention: String? = null
)

@Serializable data class FriendsFocusingReply(
    val youAreFocusing: Boolean = false,
    val friends: List<FriendFocusing> = emptyList()
)
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

@Serializable data class TodoInput(val text: String)
@Serializable data class StartSessionWithTodosReq(
    val subjectId: String? = null,
    val plannedMin: Int,
    val intention: String? = null,
    val todos: List<TodoInput>? = null
)
@Serializable data class StartSessionWithTodosReply(val sessionId: String, val serverStartTs: Long, val todoIds: List<String> = emptyList())
@Serializable data class CompleteSessionWithTodosReq(
    val sessionId: String,
    val selfRating: Int? = null,
    val doneTodoIds: List<String>? = null
)
@Serializable data class Todo(val id: String, val text: String, val done: Boolean, val ord: Int)

@Serializable data class StrokeDto(val color: String, val width: Float, val erase: Boolean = false, val points: List<List<Float>>)
@Serializable data class Note(
    val id: String,
    val subjectId: String? = null,
    val title: String = "",
    val strokes: List<StrokeDto> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)
@Serializable data class CreateNoteReq(val subjectId: String? = null, val title: String? = null, val strokes: List<StrokeDto>? = null)
@Serializable data class UpdateNoteReq(val title: String? = null, val subjectId: String? = null, val strokes: List<StrokeDto>? = null)
@Serializable data class CreateSubjectReq(val name: String, val color: String)
