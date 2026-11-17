package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AirCareDao {
    @Query("SELECT * FROM family_profile WHERE id = 1")
    fun observeFamilyProfile(): Flow<FamilyProfileEntity?>

    @Query("SELECT * FROM family_profile WHERE id = 1")
    suspend fun getFamilyProfileOnce(): FamilyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFamilyProfile(profile: FamilyProfileEntity)

    @Query("SELECT * FROM challenge_days ORDER BY dayNumber ASC")
    fun observeChallengeDays(): Flow<List<ChallengeDayEntity>>

    @Query("SELECT COUNT(*) FROM challenge_days")
    suspend fun getChallengeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallengeDays(days: List<ChallengeDayEntity>)

    @Query("UPDATE challenge_days SET isCompleted = :completed WHERE dayNumber = :dayNumber")
    suspend fun setChallengeDayCompleted(dayNumber: Int, completed: Boolean)

    @Query("UPDATE challenge_days SET isCompleted = 0")
    suspend fun resetAllChallengeDays()

    @Query("SELECT * FROM community_posts ORDER BY isDemoData ASC, createdAtMillis DESC")
    fun observeCommunityPosts(): Flow<List<CommunityPostEntity>>

    @Query("SELECT COUNT(*) FROM community_posts")
    suspend fun getCommunityPostCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunityPosts(posts: List<CommunityPostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunityPost(post: CommunityPostEntity)

    @Query("UPDATE community_posts SET isJoined = :joined, participantsCount = :count WHERE id = :postId")
    suspend fun updateCommunityJoin(postId: Long, joined: Boolean, count: Int)
}
