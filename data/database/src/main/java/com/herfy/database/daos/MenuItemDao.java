package com.herfy.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.herfy.database.entities.MenuItemDb;
import com.herfy.utils.constants.Values;

import java.util.List;

@Dao
public interface MenuItemDao {

    @Query("SELECT * FROM " + Values.TABLE_MENU_ITEMS)
    List<MenuItemDb> getAll();

    @Query("SELECT * FROM " + Values.TABLE_MENU_ITEMS + " WHERE id IN (:ids)")
    List<MenuItemDb> findAllByIds(String[] ids);

    @Query("SELECT * FROM " + Values.TABLE_MENU_ITEMS + " WHERE menu_category_id = :menuCategoryId")
    List<MenuItemDb> findAllByMenuCategoryId(String menuCategoryId);

    @Query("SELECT id FROM " + Values.TABLE_MENU_ITEMS + " WHERE menu_category_id = :menuCategoryId")
    List<String> findAllIdsByMenuCategoryId(String menuCategoryId);

    @Query("SELECT * FROM " + Values.TABLE_MENU_ITEMS + " WHERE id = :id LIMIT 1")
    MenuItemDb findById(String id);

    @Query("SELECT * FROM " + Values.TABLE_MENU_ITEMS + " WHERE application_id = :applicationId AND id = :id LIMIT 1")
    MenuItemDb findByApplicationAndId(String applicationId, String id);

    @Query("SELECT * FROM " + Values.TABLE_MENU_ITEMS + " WHERE name LIKE :name")
    List<MenuItemDb> findByName(String name);

    @Query("SELECT * FROM " + Values.TABLE_MENU_ITEMS + " WHERE application_id = :applicationId AND name = :name LIMIT 1")
    MenuItemDb findByApplicationAndName(String applicationId, String name);

    @Query("SELECT EXISTS(SELECT * FROM " + Values.TABLE_MENU_ITEMS + " WHERE id = :id)")
    boolean has(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(MenuItemDb... menuItems);

    @Query("DELETE FROM " + Values.TABLE_MENU_ITEMS)
    void deleteAll();

    @Query("DELETE FROM " + Values.TABLE_MENU_ITEMS + " WHERE application_id = :applicationId")
    void deleteByApplication(String applicationId);

    @Delete
    void delete(MenuItemDb menuItem);

}