package com.herfy.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.herfy.data.converters.LocationPriceListConverter;
import com.herfy.data.converters.StringListConverter;
import com.herfy.data.converters.TranslatedEntityConverter;
import com.herfy.data.daos.*;
import com.herfy.data.entities.AllergenDb;
import com.herfy.data.entities.FacilityEntity;
import com.herfy.data.entities.IngredientDb;
import com.herfy.data.entities.MenuCategoryDb;
import com.herfy.data.entities.MenuCategoryEntity;
import com.herfy.data.entities.MenuItemDb;
import com.herfy.data.entities.MenuItemEntity;
import com.herfy.data.entities.ModifierGroupDb;
import com.herfy.data.entities.ModifierItemDb;
import com.herfy.data.entities.StoreDb;

import dev.matrix.roomigrant.GenerateRoomMigrations;

@Database(entities = {
        MenuCategoryDb.class,
        MenuItemDb.class,
        ModifierGroupDb.class,
        ModifierItemDb.class,
        IngredientDb.class,
        AllergenDb.class,
        StoreDb.class,
        FacilityEntity.class,
        MenuCategoryEntity.class,
        MenuItemEntity.class
}, version = 1)
@TypeConverters(value = {
        LocationPriceListConverter.class,
        StringListConverter.class,
        TranslatedEntityConverter.class
    }
)
@GenerateRoomMigrations(rules = {})
public abstract class AppDatabase extends RoomDatabase {

    public static AppDatabase instance;

    public static void init(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context, AppDatabase.class, context.getPackageName()
                            .replace(".", "_") + "_db")
                            .addMigrations(AppDatabase_Migrations.INSTANCE.build())
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
        }
    }

    public static AppDatabase initWith(Context context){
        if(instance == null){
            instance = Room.databaseBuilder(context, AppDatabase.class, context.getPackageName()
                            .replace(".", "_") + "_db")
                    .addMigrations(AppDatabase_Migrations.INSTANCE.build())
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }

        return instance;
    }

    public static synchronized AppDatabase getInstance() {
        return instance;
    }

    public abstract MenuCategoryDao menuCategoryDao();

    public abstract MenuItemDao menuItemDao();

    public abstract ModifierGroupDao modifierGroupDao();

    public abstract ModifierItemDao modifierItemDao();

    public abstract IngredientDao ingredientDao();

    public abstract AllergenDao allergenDao();

    public abstract StoreDao storeDao();

    public abstract FacilityDao facilityDao();

    public abstract MenuCategoryEntityDao menuCategoryEntityDao();
    public abstract MenuItemEntityDao menuItemEntityDao();

}
