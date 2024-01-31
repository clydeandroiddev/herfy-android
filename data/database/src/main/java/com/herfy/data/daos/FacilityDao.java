package com.herfy.data.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.herfy.data.constants.Values;
import com.herfy.data.entities.FacilityEntity;

import java.util.List;

@Dao
public interface FacilityDao {

    @Query("SELECT * FROM " + Values.TABLE_FACILITIES)
    List<FacilityEntity> getAll();

    @Query("SELECT * FROM " + Values.TABLE_FACILITIES + " WHERE id IN (:ids)")
    List<FacilityEntity> findAllByIds(String[] ids);

    @Query("SELECT * FROM " + Values.TABLE_FACILITIES + " WHERE id = :id LIMIT 1")
    FacilityEntity findById(String id);

    @Query("SELECT * FROM " + Values.TABLE_FACILITIES + " WHERE application_id = :applicationId")
    List<FacilityEntity> findAllByApplicationId(String applicationId);

    @Query("SELECT EXISTS(SELECT * FROM " + Values.TABLE_FACILITIES + " WHERE id = :id)")
    boolean has(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(FacilityEntity... allergens);

    @Query("DELETE FROM " + Values.TABLE_FACILITIES)
    void deleteAll();

    @Query("DELETE FROM " + Values.TABLE_FACILITIES + " WHERE application_id = :applicationId")
    void deleteByApplication(String applicationId);

    @Delete
    void delete(FacilityEntity store);

}