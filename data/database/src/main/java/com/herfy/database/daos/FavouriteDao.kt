package com.herfy.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.herfy.utils.constants.Values
import com.skylinedynamics.solosdk.api.models.objects.Favorite
@Dao
interface FavouriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(favourites: List<Favorite>)

    @Query("SELECT * FROM " + Values.TABLE_FAVOURITES)
    fun getAll(): List<Favorite?>
}