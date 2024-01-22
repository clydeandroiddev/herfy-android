package com.herfy.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.herfy.constants.Values;
import com.herfy.database.entities.ModifierGroupDb;

import java.util.List;

@Dao
public interface ModifierGroupDao {

    @Query("SELECT * FROM " + Values.TABLE_MODIFIER_GROUPS)
    List<ModifierGroupDb> getAll();

    @Query("SELECT * FROM " + Values.TABLE_MODIFIER_GROUPS + " WHERE id IN (:ids)")
    List<ModifierGroupDb> findAllByIds(String[] ids);

    @Query("SELECT * FROM " + Values.TABLE_MODIFIER_GROUPS + " WHERE application_id = :applicationId AND id IN (:ids)")
    List<ModifierGroupDb> findAllByApplicationAndIds(String applicationId, String[] ids);

    @Query("SELECT * FROM " + Values.TABLE_MODIFIER_GROUPS + " WHERE id = :id LIMIT 1")
    ModifierGroupDb findById(String id);

    @Query("SELECT * FROM " + Values.TABLE_MODIFIER_GROUPS + " WHERE menu_item_id = :menuItemId AND type = :type")
    List<ModifierGroupDb> findAllByMenuItemId(String menuItemId, int type);

    @Query("SELECT EXISTS(SELECT * FROM " + Values.TABLE_MODIFIER_GROUPS + " WHERE id = :id)")
    boolean has(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(ModifierGroupDb... modifierGroups);

    @Query("DELETE FROM " + Values.TABLE_MODIFIER_GROUPS)
    void deleteAll();

    @Query("DELETE FROM " + Values.TABLE_MODIFIER_GROUPS + " WHERE application_id = :applicationId")
    void deleteByApplication(String applicationId);

    @Delete
    void delete(ModifierGroupDb modifierGroup);

    @Query("DELETE FROM " + Values.TABLE_MODIFIER_GROUPS + " WHERE menu_item_id = :menuItemId")
    void delete(String menuItemId);

}