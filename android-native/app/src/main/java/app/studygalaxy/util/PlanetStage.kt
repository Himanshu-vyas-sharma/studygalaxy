package app.studygalaxy.util

/**
 * A subject's planet terraforms as focused minutes accumulate. Thresholds are
 * deliberately reachable: a student doing ~40 min a day sees a new stage every
 * few days for the first fortnight, which is what makes week three feel
 * different from day one.
 */
enum class PlanetStage(
    val label: String,
    val minMinutes: Int,
    val hasAtmosphere: Boolean = false,
    val hasWater: Boolean = false,
    val hasRings: Boolean = false,
    val moons: Int = 0,
    val hasLife: Boolean = false
) {
    Barren("Barren rock", 0),
    Crust("Cooling crust", 60),
    Atmosphere("Atmosphere forming", 180, hasAtmosphere = true),
    Oceans("Oceans", 420, hasAtmosphere = true, hasWater = true),
    Rings("Ringed world", 900, hasAtmosphere = true, hasWater = true, hasRings = true),
    Moons("Moons captured", 1800, hasAtmosphere = true, hasWater = true, hasRings = true, moons = 2),
    Life("Life", 3600, hasAtmosphere = true, hasWater = true, hasRings = true, moons = 3, hasLife = true);

    companion object {
        private val descending = entries.sortedByDescending { it.minMinutes }

        fun forMinutes(totalMin: Int): PlanetStage =
            descending.firstOrNull { totalMin >= it.minMinutes } ?: Barren

        /** Minutes still needed for the next stage, or null at the final stage. */
        fun minutesToNext(totalMin: Int): Int? {
            val next = entries.firstOrNull { it.minMinutes > totalMin } ?: return null
            return next.minMinutes - totalMin
        }

        fun next(totalMin: Int): PlanetStage? = entries.firstOrNull { it.minMinutes > totalMin }
    }
}
