package com.herfy.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.herfy.constants.Values;
import com.herfy.database.entities.IngredientDb;

import java.util.List;

@Dao
public interface IngredientDao {

    @Query("SELECT * FROM " + Values.TABLE_INGREDIENTS)
    List<IngredientDb> getAll();

    @Query("SELECT * FROM " + Values.TABLE_INGREDIENTS + " WHERE id IN (:ids)")
    List<IngredientDb> findAllByIds(String[] ids);

    @Query("SELECT * FROM " + Values.TABLE_INGREDIENTS + " WHERE application_id = :applicationId AND id IN (:ids)")
    List<IngredientDb> findAllByApplicationAndIds(String applicationId, String[] ids);

    @Query("SELECT * FROM " + Values.TABLE_INGREDIENTS + " WHERE id = :id LIMIT 1")
    IngredientDb findById(String id);

    @Query("SELECT * FROM " + Values.TABLE_INGREDIENTS + " WHERE menu_item_id = :menuItemId")
    List<IngredientDb> findAllByMenuItemId(String menuItemId);

    @Query("SELECT EXISTS(SELECT * FROM " + Values.TABLE_INGREDIENTS + " WHERE id = :id)")
    boolean has(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(IngredientDb... ingredients);

    @Query("DELETE FROM " + Values.TABLE_INGREDIENTS)
    void deleteAll();

    @Query("DELETE FROM " + Values.TABLE_INGREDIENTS + " WHERE application_id = :applicationId")
    void deleteByApplication(String applicationId);

    @Delete
    void delete(IngredientDb ingredient);

    @Query("DELETE FROM " + Values.TABLE_INGREDIENTS + " WHERE menu_item_id = :menuItemId")
    void delete(String menuItemId);

}