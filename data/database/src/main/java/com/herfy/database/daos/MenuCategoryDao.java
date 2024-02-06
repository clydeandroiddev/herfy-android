package com.herfy.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.herfy.database.entities.MenuCategoryDb;
import com.herfy.utils.constants.Values;

import java.util.List;

@Dao
public interface MenuCategoryDao {

    @Query("SELECT * FROM " + Values.TABLE_MENU_CATEGORIES)
    List<MenuCategoryDb> getAll();

    @Query("SELECT * FROM " + Values.TABLE_MENU_CATEGORIES + " WHERE id IN (:ids)")
    List<MenuCategoryDb> findAllByIds(String[] ids);

    @Query("SELECT * FROM " + Values.TABLE_MENU_CATEGORIES + " WHERE id = :id LIMIT 1")
    MenuCategoryDb findById(String id);

    @Query("SELECT * FROM " + Values.TABLE_MENU_CATEGORIES + " WHERE application_id = :applicationId AND id = :id LIMIT 1")
    MenuCategoryDb findByApplicationAndId(String applicationId, String id);

    @Query("SELECT EXISTS(SELECT * FROM " + Values.TABLE_MENU_CATEGORIES + ")")
    boolean has();

    @Query("SELECT EXISTS(SELECT * FROM " + Values.TABLE_MENU_CATEGORIES + " WHERE id = :id)")
    boolean has(String id);

    @Query("SELECT COUNT(id) FROM " + Values.TABLE_MENU_CATEGORIES)
    int getCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(MenuCategoryDb... menuCategories);

    @Query("DELETE FROM " + Values.TABLE_MENU_CATEGORIES)
    void deleteAll();

    @Query("DELETE FROM " + Values.TABLE_MENU_CATEGORIES + " WHERE application_id = :applicationId")
    void deleteByApplication(String applicationId);

    @Delete
    void delete(MenuCategoryDb menuCategory);

}