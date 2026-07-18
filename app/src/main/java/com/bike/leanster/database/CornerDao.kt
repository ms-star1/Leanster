package com.bike.leanster.database

import androidx.room.*
import com.bike.leanster.database.entities.CornerEntity
import com.bike.leanster.database.entities.CornerPbEntity
import com.bike.leanster.database.entities.SessionCornerEntity

@Dao
interface CornerDao {

    // --- Corners ---

    @Query("SELECT * FROM corners WHERE centroidLat BETWEEN :minLat AND :maxLat AND centroidLng BETWEEN :minLng AND :maxLng")
    suspend fun getCornersInBounds(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<CornerEntity>

    @Insert
    suspend fun insertCorner(corner: CornerEntity): Long

    @Query("UPDATE corners SET timesRidden = timesRidden + 1 WHERE id = :id")
    suspend fun incrementRideCount(id: Long)

    // --- Personal Bests ---

    @Query("SELECT * FROM corner_pbs WHERE cornerId = :cornerId LIMIT 1")
    suspend fun getPbForCorner(cornerId: Long): CornerPbEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPb(pb: CornerPbEntity)

    @Query("UPDATE corner_pbs SET bestLean = :lean, bestSpeed = :speed, bestSessionId = :sessionId, achievedAt = :timestamp WHERE cornerId = :cornerId")
    suspend fun updatePb(cornerId: Long, lean: Float, speed: Double, sessionId: String, timestamp: Long)

    // --- Session Corners ---

    @Insert
    suspend fun insertSessionCorner(sc: SessionCornerEntity)

    @Query("SELECT cornerId FROM session_corners WHERE sessionId = :sessionId")
    suspend fun getCornerIdsForSession(sessionId: String): List<Long>

    @Query("SELECT * FROM session_corners WHERE sessionId = :sessionId ORDER BY cornerTimestamp ASC")
    suspend fun getSessionCorners(sessionId: String): List<SessionCornerEntity>

    @Query("SELECT * FROM session_corners WHERE cornerId = :cornerId ORDER BY leanAchieved DESC LIMIT 20")
    suspend fun getHistoryForCorner(cornerId: Long): List<SessionCornerEntity>

    @Query("SELECT * FROM session_corners WHERE sessionId IN (:sessionIds)")
    suspend fun getSessionCornersForSessions(sessionIds: List<String>): List<SessionCornerEntity>

    @Query("""
        SELECT sessionId FROM session_corners
        WHERE cornerId IN (:cornerIds) AND sessionId != :excludeSessionId
        GROUP BY sessionId
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """)
    suspend fun findBestMatchingSessionId(cornerIds: List<Long>, excludeSessionId: String): String?

    @Query("SELECT COUNT(*) FROM session_corners WHERE sessionId = :sessionId")
    suspend fun getCornerCountForSession(sessionId: String): Int

    // --- Report Card aggregation ---

    @Query("""
        SELECT AVG(sc.leanAchieved * 1.0 / pb.bestLean)
        FROM session_corners sc
        INNER JOIN corner_pbs pb ON sc.cornerId = pb.cornerId
        WHERE sc.sessionId = :sessionId AND pb.bestLean > 0
    """)
    suspend fun getConsistencyRatioForSession(sessionId: String): Float?
}
