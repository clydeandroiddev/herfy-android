package com.herfy.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.herfy.constants.Values;
import com.herfy.database.entities.AllergenDb;

import java.util.List;

@Dao
public interface AllergenDao {

    @Query("SELECT * FROM " + Values.TABLE_ALLERGENS)
    List<AllergenDb> getAll();

    @Query("SELECT * FROM " + Values.TABLE_ALLERGENS + " WHERE id IN (:ids)")
    List<AllergenDb> findAllByIds(String[] ids);

    @Query("SELECT * FROM " + Values.TABLE_ALLERGENS + " WHERE application_id = :applicationId AND id IN (:ids)")
    List<AllergenDb> findAllByApplicationAndIds(String applicationId, String[] ids);

    @Query("SELECT * FROM " + Values.TABLE_ALLERGENS + " WHERE id = :id LIMIT 1")
    AllergenDb findById(String id);

    @Query("SELECT * FROM " + Values.TABLE_ALLERGENS + " WHERE menu_item_id = :menuItemId")
    List<AllergenDb> findAllByMenuItemId(String menuItemId);

    @Query("SELECT EXISTS(SELECT * FROM " + Values.TABLE_ALLERGENS + " WHERE id = :id)")
    boolean has(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(AllergenDb... allergens);

    @Query("DELETE FROM " + Values.TABLE_ALLERGENS)
    void deleteAll();

    @Query("DELETE FROM " + Values.TABLE_ALLERGENS + " WHERE application_id = :applicationId")
    void deleteByApplication(String applicationId);

    @Delete
    void delete(AllergenDb allergen);

    @Query("DELETE FROM " + Values.TABLE_ALLERGENS + " WHERE menu_item_id = :menuItemId")
    void delete(String menuItemId);

}