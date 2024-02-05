package com.herfy.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.herfy.database.entities.MenuItemEntity;
import com.herfy.utils.constants.Values;

import java.util.List;

@Dao
public interface MenuItemEntityDao {

    @Query("SELECT * FROM " + Values.TABLE_MENU_ITEMS_ENTITY)
    List<MenuItemEntity> getAll();

    @Query("SELECT * FROM " + Values.TABLE_MENU_ITEMS_ENTITY + " WHERE id IN (:ids)")
    List<MenuItemEntity> findAllByIds(String[] ids);

    @Query("SELECT * FROM " + Values.TABLE_MENU_ITEMS_ENTITY + " WHERE application_id = :applicationId AND menu_category_id = :menuCategoryId")
    List<MenuItemEntity> findAllByMenuCategoryId(String applicationId, String menuCategoryId);

    @Query("SELECT id FROM " + Values.TABLE_MENU_ITEMS_ENTITY + " WHERE menu_category_id = :menuCategoryId")
    List<String> findAllIdsByMenuCategoryId(String menuCategoryId);

    @Query("SELECT * FROM " + Values.TABLE_MENU_ITEMS_ENTITY + " WHERE application_id = :applicationId AND id = :id LIMIT 1")
    MenuItemEntity findById(String applicationId, String id);

    /*@Query("SELECT * FROM " + Values.TABLE_MENU_ITEMS_ENTITY + " WHERE name LIKE :name")
    List<MenuItemDb> findByName(String name);*/

    @Query("SELECT EXISTS(SELECT * FROM " + Values.TABLE_MENU_ITEMS_ENTITY + " WHERE id = :id)")
    boolean has(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(MenuItemEntity... menuItems);

    @Query("DELETE FROM " + Values.TABLE_MENU_ITEMS_ENTITY)
    void deleteAll();

    @Query("DELETE FROM " + Values.TABLE_MENU_ITEMS_ENTITY + " WHERE application_id = :applicationId")
    void deleteByApplication(String applicationId);

    @Delete
    void delete(MenuItemEntity menuItem);

}