package app.studygalaxy.notifications

/**
 * Nudge copy. Kept on-device so a reminder still fires with no network, and so
 * the server never needs to know a user's notification schedule.
 *
 * Tone rule: pressure the behaviour, never the person. "Your friend is
 * studying" works because it is a fact. "You are lazy" would get the app
 * uninstalled.
 */
object Motivation {

    /** Shown when friends are in a live session and the user is not. */
    fun friendStudying(names: List<String>, intention: String?): Pair<String, String> {
        val title = when (names.size) {
            0 -> "Someone is studying"
            1 -> "${names[0]} is studying right now"
            2 -> "${names[0]} and ${names[1]} are studying"
            else -> "${names[0]} and ${names.size - 1} others are studying"
        }
        val body = if (!intention.isNullOrBlank()) {
            "\"$intention\" — what are you working on?"
        } else {
            listOf(
                "What are you doing?",
                "Your galaxy is sitting still. Theirs isn't.",
                "25 minutes and you're in the room with them.",
                "Don't be the one explaining later.",
                "Join them. One star is one session away.",
            ).random()
        }
        return title to body
    }

    /** Shown when the app has gone unused for a while. */
    fun idleNudge(streakDays: Int): Pair<String, String> {
        if (streakDays > 0) {
            return "Your ${streakDays}-day streak is still alive" to listOf(
                "One session today keeps it. Miss today and it resets to zero.",
                "You've shown up $streakDays days straight. Don't break it now.",
                "$streakDays days of proof you can do this. Add one more.",
            ).random()
        }
        return listOf(
            "Your galaxy is dark" to "No stars today. 25 minutes changes that.",
            "Your planets are drifting" to "They terraform only while you focus. Start one session.",
            "Nothing grew today" to "The smallest session still counts. 25 minutes.",
            "Still time to make today count" to "One focus session. That's the whole ask.",
            "Your Physics planet misses you" to "Pick any subject and give it 25 minutes.",
        ).random()
    }
}
