package com.test.healthbox_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.test.healthbox_app.data.local.entity.ParametersEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParameterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSession(session: ParametersEntity)

    @Query("SELECT * FROM session WHERE id = 0 LIMIT 1")
    suspend fun get(): ParametersEntity?

    @Query("DELETE FROM session")
    suspend fun clear()

    @Query("SELECT * FROM session WHERE id = 0 LIMIT 1")

    fun observe(): Flow<ParametersEntity?>
}