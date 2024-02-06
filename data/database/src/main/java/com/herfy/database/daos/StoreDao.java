package com.herfy.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.herfy.database.entities.StoreDb;
import com.herfy.utils.constants.Values;

import java.util.List;

@Dao
public interface StoreDao {

    @Query("SELECT * FROM " + Values.TABLE_STORES)
    List<StoreDb> getAll();

    @Query("SELECT * FROM " + Values.TABLE_STORES + " WHERE id IN (:ids)")
    List<StoreDb> findAllByIds(String[] ids);

    @Query("SELECT * FROM " + Values.TABLE_STORES + " WHERE id = :id LIMIT 1")
    StoreDb findById(String id);

    @Query("SELECT * FROM " + Values.TABLE_STORES + " WHERE application_id = :applicationId")
    List<StoreDb> findAllByApplicationId(String applicationId);

    @Query("SELECT * FROM " + Values.TABLE_STORES + " WHERE menu_item_id = :menuItemId")
    List<StoreDb> findAllByMenuItemId(String menuItemId);

    @Query("SELECT EXISTS(SELECT * FROM " + Values.TABLE_STORES + " WHERE id = :id)")
    boolean has(String id);

    @Query("SELECT EXISTS(SELECT * FROM " + Values.TABLE_STORES + " WHERE application_id = :applicationId)")
    boolean hasByApplication(String applicationId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(StoreDb... allergens);

    @Query("DELETE FROM " + Values.TABLE_STORES)
    void deleteAll();

    @Query("DELETE FROM " + Values.TABLE_STORES + " WHERE application_id = :applicationId")
    void deleteByApplication(String applicationId);

    @Delete
    void delete(StoreDb store);

    @Query("DELETE FROM " + Values.TABLE_STORES + " WHERE menu_item_id = :menuItemId")
    void delete(String menuItemId);

}