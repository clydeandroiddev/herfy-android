package com.herfy.data.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.herfy.data.constants.Values;
import com.herfy.data.entities.ModifierItemDb;

import java.util.List;

@Dao
public interface ModifierItemDao {

    @Query("SELECT * FROM " + Values.TABLE_MODIFIER_ITEMS)
    List<ModifierItemDb> getAll();

    @Query("SELECT * FROM " + Values.TABLE_MODIFIER_ITEMS + " WHERE id IN (:ids)")
    List<ModifierItemDb> findAllByIds(String[] ids);

    @Query("SELECT * FROM " + Values.TABLE_MODIFIER_ITEMS + " WHERE application_id = :applicationId AND id IN (:ids)")
    List<ModifierItemDb> findAllByApplicationAndIds(String applicationId, String[] ids);

    @Query("SELECT * FROM " + Values.TABLE_MODIFIER_ITEMS + " WHERE id = :id LIMIT 1")
    ModifierItemDb findById(String id);

    @Query("SELECT * FROM " + Values.TABLE_MODIFIER_ITEMS + " WHERE menu_item_id = :menuItemId AND modifier_group_id = :modifierGroupId")
    List<ModifierItemDb> findAllByMenuItemIdAndModifierGroupId(String menuItemId, String modifierGroupId);

    @Query("SELECT EXISTS(SELECT * FROM " + Values.TABLE_MODIFIER_ITEMS + " WHERE id = :id)")
    boolean has(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(ModifierItemDb... modifierItems);

    @Query("DELETE FROM " + Values.TABLE_MODIFIER_ITEMS)
    void deleteAll();

    @Query("DELETE FROM " + Values.TABLE_MODIFIER_ITEMS + " WHERE application_id = :applicationId")
    void deleteByApplication(String applicationId);

    @Delete
    void delete(ModifierItemDb modifierItem);

    @Query("DELETE FROM " + Values.TABLE_MODIFIER_ITEMS + " WHERE menu_item_id = :menuItemId")
    void delete(String menuItemId);

}