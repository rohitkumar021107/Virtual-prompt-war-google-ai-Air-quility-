package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AirCareDao
import com.example.data.local.AirCareDatabase
import com.example.data.local.CommunityPostEntity
import com.example.data.local.FamilyProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AirCareDatabaseTest {

    private lateinit var db: AirCareDatabase
    private lateinit var dao: AirCareDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AirCareDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.airCareDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `seedInitialDataIfNeeded populates defaults once and is idempotent`() = runBlocking {
        db.seedInitialDataIfNeeded()
        db.seedInitialDataIfNeeded()

        val profile = dao.getFamilyProfileOnce()
        assertNotNull(profile)
        assertEquals(4, profile?.memberCount)

        val days = dao.observeChallengeDays().first()
        assertEquals(7, days.size)
        assertTrue(days.all { !it.isCompleted })

        val posts = dao.observeCommunityPosts().first()
        assertEquals(4, posts.size)
        assertTrue(posts.all { it.isDemoData })
    }

    @Test
    fun `family profile update overwrites single row id 1`() = runBlocking {
        db.seedInitialDataIfNeeded()
        val updated = FamilyProfileEntity(
            id = 1,
            locationName = "Pune, India",
            memberCount = 5,
            workingAdults = 2,
            children = 2,
            olderAdults = 1,
            outdoorWorkers = 0,
            dailyTravelKm = 24,
            transportMethod = "Metro / Bus",
            budgetPreference = "Low",
            mainConcerns = "School bus dust"
        )
        dao.saveFamilyProfile(updated)

        val stored = dao.getFamilyProfileOnce()
        assertEquals("Pune, India", stored?.locationName)
        assertEquals(5, stored?.memberCount)
        assertEquals("Low", stored?.budgetPreference)
    }

    @Test
    fun `challenge days toggle and reset persist properly`() = runBlocking {
        db.seedInitialDataIfNeeded()
        dao.setChallengeDayCompleted(1, true)
        dao.setChallengeDayCompleted(3, true)

        var days = dao.observeChallengeDays().first()
        assertTrue(days.first { it.dayNumber == 1 }.isCompleted)
        assertTrue(days.first { it.dayNumber == 3 }.isCompleted)
        assertFalse(days.first { it.dayNumber == 2 }.isCompleted)

        dao.resetAllChallengeDays()
        days = dao.observeChallengeDays().first()
        assertTrue(days.none { it.isCompleted })
    }

    @Test
    fun `community posts order local entries before demo data and update pledges`() = runBlocking {
        db.seedInitialDataIfNeeded()
        dao.insertCommunityPost(
            CommunityPostEntity(
                category = "CARPOOL",
                title = "Banjara Hills School Pool",
                description = "3 seats shared daily",
                area = "Hyderabad",
                authorLabel = "Local Household Entry",
                isDemoData = false,
                participantsCount = 1,
                isJoined = true,
                createdAtMillis = System.currentTimeMillis()
            )
        )

        val posts = dao.observeCommunityPosts().first()
        assertEquals(5, posts.size)
        assertFalse(posts.first().isDemoData)
        assertEquals("Banjara Hills School Pool", posts.first().title)

        val demoPost = posts.last()
        dao.updateCommunityJoin(demoPost.id, true, demoPost.participantsCount + 1)
        val afterJoin = dao.observeCommunityPosts().first().first { it.id == demoPost.id }
        assertTrue(afterJoin.isJoined)
        assertEquals(demoPost.participantsCount + 1, afterJoin.participantsCount)
    }
}
