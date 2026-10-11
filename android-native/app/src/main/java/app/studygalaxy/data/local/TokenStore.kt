package app.studygalaxy.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "sg_prefs")

object TokenStore {
    private val JWT = stringPreferencesKey("jwt")
    private val DEVICE_ID = stringPreferencesKey("deviceId")
    private val NAME_CLAIMED = stringPreferencesKey("nameClaimed")
    private val ONBOARDED = stringPreferencesKey("onboarded")
    private val ONBOARDING_JSON = stringPreferencesKey("onboardingJson")

    fun jwtFlow(ctx: Context): Flow<String?> = ctx.dataStore.data.map { it[JWT] }
    fun nameClaimedFlow(ctx: Context): Flow<Boolean> = ctx.dataStore.data.map { it[NAME_CLAIMED] == "1" }

    suspend fun isOnboarded(ctx: Context): Boolean =
        ctx.dataStore.data.firstOrNull()?.get(ONBOARDED) == "1"

    suspend fun saveOnboarding(ctx: Context, json: String) {
        ctx.dataStore.edit {
            it[ONBOARDED] = "1"
            it[ONBOARDING_JSON] = json
        }
    }

    suspend fun getOnboardingJson(ctx: Context): String? =
        ctx.dataStore.data.firstOrNull()?.get(ONBOARDING_JSON)

    suspend fun getJwt(ctx: Context): String? = ctx.dataStore.data.firstOrNull()?.get(JWT)

    suspend fun setJwt(ctx: Context, token: String?) {
        ctx.dataStore.edit { prefs ->
            if (token == null) prefs.remove(JWT) else prefs[JWT] = token
        }
    }

    suspend fun markNameClaimed(ctx: Context) {
        ctx.dataStore.edit { it[NAME_CLAIMED] = "1" }
    }

    suspend fun getOrCreateDeviceId(ctx: Context): String {
        val existing = ctx.dataStore.data.firstOrNull()?.get(DEVICE_ID)
        if (existing != null) return existing
        val new = UUID.randomUUID().toString()
        ctx.dataStore.edit { it[DEVICE_ID] = new }
        return new
    }

    suspend fun clear(ctx: Context) {
        ctx.dataStore.edit { it.clear() }
    }
}
