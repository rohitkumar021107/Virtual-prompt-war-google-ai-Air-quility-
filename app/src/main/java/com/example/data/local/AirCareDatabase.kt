package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FamilyProfileEntity::class,
        ChallengeDayEntity::class,
        CommunityPostEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AirCareDatabase : RoomDatabase() {
    abstract fun airCareDao(): AirCareDao

    suspend fun seedInitialDataIfNeeded() {
        val dao = airCareDao()
        if (dao.getFamilyProfileOnce() == null) {
            dao.saveFamilyProfile(FamilyProfileEntity())
        }
        if (dao.getChallengeCount() == 0) {
            dao.insertChallengeDays(
                listOf(
                    ChallengeDayEntity(
                        dayNumber = 1,
                        title = "Avoid unnecessary vehicle trips",
                        description = "Combine short errands into a single trip or handle one errand by walking if AQI permits.",
                        tip = "Cold-start short drives emit disproportionate NOx & particulates.",
                        points = 15
                    ),
                    ChallengeDayEntity(
                        dayNumber = 2,
                        title = "Clean indoor surfaces using damp dusting",
                        description = "Use a wet mop and damp microfiber cloth instead of dry brooms or feather dusters.",
                        tip = "Dry sweeping re-suspends settled PM10 back into children's breathing height.",
                        points = 15
                    ),
                    ChallengeDayEntity(
                        dayNumber = 3,
                        title = "Use public transportation",
                        description = "Take the metro, city bus, or carpool for your work or college commute today.",
                        tip = "Each shared trip removes tailpipe congestion from busy urban corridors.",
                        points = 20
                    ),
                    ChallengeDayEntity(
                        dayNumber = 4,
                        title = "Avoid burning waste",
                        description = "Ensure zero dry leaves, paper, or household trash are burned; compost garden trimmings instead.",
                        tip = "Open waste burning triggers sharp overnight PM2.5 spikes in neighborhoods.",
                        points = 20
                    ),
                    ChallengeDayEntity(
                        dayNumber = 5,
                        title = "Check and maintain air purifier filters",
                        description = "Vacuum outer mesh pre-filters and inspect HEPA cartridges or exhaust fans in your home.",
                        tip = "Clean pre-filters restore airflow and extend HEPA cartridge lifespan.",
                        points = 15
                    ),
                    ChallengeDayEntity(
                        dayNumber = 6,
                        title = "Organize a community clean-air activity",
                        description = "Invite neighbors to a school carpool chat, tree-care watering, or no-idling drop-off pledge.",
                        tip = "Collective habits multiply emission reductions beyond one household.",
                        points = 25
                    ),
                    ChallengeDayEntity(
                        dayNumber = 7,
                        title = "Review your weekly environmental impact",
                        description = "Open the Environmental Impact Calculator and review your CO₂ avoided and next week's goal.",
                        tip = "Tracking progress keeps sustainable commutes consistent all winter.",
                        points = 20
                    )
                )
            )
        }
        if (dao.getCommunityPostCount() == 0) {
            val now = System.currentTimeMillis()
            dao.insertCommunityPosts(
                listOf(
                    CommunityPostEntity(
                        category = "CARPOOL",
                        title = "Jubilee Hills – HITEC City Morning Carpool",
                        description = "Sample Demonstration Campaign: 3-seat shared commute departing 8:20 AM weekdays along Metro corridor to reduce solo petrol cars.",
                        area = "Hyderabad (Demo)",
                        authorLabel = "Sample Demonstration Data",
                        isDemoData = true,
                        participantsCount = 1,
                        createdAtMillis = now - 86_400_000L
                    ),
                    CommunityPostEntity(
                        category = "TREE_CARE",
                        title = "KBR Perimeter Native Sapling Watering & Mulching",
                        description = "Sample Demonstration Activity: Weekly morning mulching and watering of young neem & pongamia saplings to trap coarse road dust.",
                        area = "Hyderabad (Demo)",
                        authorLabel = "Sample Demonstration Data",
                        isDemoData = true,
                        participantsCount = 1,
                        createdAtMillis = now - 72_000_000L
                    ),
                    CommunityPostEntity(
                        category = "INITIATIVE",
                        title = "No-Idling School Gate Clean Air Pledge",
                        description = "Sample Demonstration Initiative: Parents turning off engines while waiting >30 seconds outside primary school drop-off zones.",
                        area = "Secunderabad (Demo)",
                        authorLabel = "Sample Demonstration Data",
                        isDemoData = true,
                        participantsCount = 1,
                        createdAtMillis = now - 54_000_000L
                    ),
                    CommunityPostEntity(
                        category = "WASTE_REPORT",
                        title = "Open Leaf Burning Reporting Checklist",
                        description = "Sample Demonstration Guide: Document exact landmark, time, and avoid confronting burners directly; submit via CPCB Sameer App or local municipal helpline (e.g., MyGHMC / 155304).",
                        area = "Municipal Channels (Demo)",
                        authorLabel = "Sample Demonstration Data",
                        isDemoData = true,
                        participantsCount = 0,
                        createdAtMillis = now - 36_000_000L
                    )
                )
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AirCareDatabase? = null

        fun getInstance(context: Context): AirCareDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AirCareDatabase::class.java,
                    "aircare_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
