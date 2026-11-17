package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_profile")
data class FamilyProfileEntity(
    @PrimaryKey val id: Int = 1,
    val locationName: String = "Hyderabad, India",
    val memberCount: Int = 4,
    val workingAdults: Int = 2,
    val children: Int = 1,
    val olderAdults: Int = 1,
    val outdoorWorkers: Int = 0,
    val dailyTravelKm: Int = 18,
    val transportMethod: String = "Private Petrol Car",
    val budgetPreference: String = "Moderate",
    val mainConcerns: String = "Winter morning smog, school commute, indoor dust"
)

@Entity(tableName = "challenge_days")
data class ChallengeDayEntity(
    @PrimaryKey val dayNumber: Int,
    val title: String,
    val description: String,
    val tip: String,
    val points: Int,
    val isCompleted: Boolean = false
)

@Entity(tableName = "community_posts")
data class CommunityPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: String, // INITIATIVE, CARPOOL, TREE_CARE, WASTE_REPORT
    val title: String,
    val description: String,
    val area: String,
    val authorLabel: String,
    val isDemoData: Boolean,
    val participantsCount: Int,
    val isJoined: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
