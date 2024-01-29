package com.herfy.data.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.herfy.data.constants.Values;
import com.herfy.data.entities.MenuCategoryEntity;

import java.util.List;

@Dao
public interface MenuCategoryEntityDao {

    @Query("SELECT * FROM " + Values.TABLE_MENU_CATEGORIES_ENTITY)
    List<MenuCategoryEntity> getAll();

    @Query("SELECT * FROM " + Values.TABLE_MENU_CATEGORIES_ENTITY + " WHERE application_id = :applicationId")
    List<MenuCategoryEntity> getAllByApplication(String applicationId);

    @Query("SELECT * FROM " + Values.TABLE_MENU_CATEGORIES_ENTITY + " WHERE id IN (:ids)")
    List<MenuCategoryEntity> findAllByIds(String[] ids);

    @Query("SELECT * FROM " + Values.TABLE_MENU_CATEGORIES_ENTITY + " WHERE application_id = :applicationId AND id = :id LIMIT 1")
    MenuCategoryEntity findById(String applicationId, String id);

    @Query("SELECT EXISTS(SELECT * FROM " + Values.TABLE_MENU_CATEGORIES_ENTITY + ")")
    boolean has();

    @Query("SELECT EXISTS(SELECT * FROM " + Values.TABLE_MENU_CATEGORIES_ENTITY + " WHERE id = :id)")
    boolean has(String id);

    @Query("SELECT EXISTS(SELECT * FROM " + Values.TABLE_MENU_CATEGORIES_ENTITY + " WHERE application_id = :applicationId)")
    boolean hasByApplication(String applicationId);

    @Query("SELECT COUNT(id) FROM " + Values.TABLE_MENU_CATEGORIES_ENTITY)
    int getCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(MenuCategoryEntity... menuCategories);

    @Query("DELETE FROM " + Values.TABLE_MENU_CATEGORIES_ENTITY)
    void deleteAll();

    @Query("DELETE FROM " + Values.TABLE_MENU_CATEGORIES_ENTITY + " WHERE application_id = :applicationId")
    void deleteByApplication(String applicationId);

    @Delete
    void delete(MenuCategoryEntity menuCategory);

}