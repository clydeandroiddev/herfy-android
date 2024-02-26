package com.herfy.repository

import com.herfy.database.AppDatabase
import com.herfy.database.entities.MenuCategoryEntity.Companion.toDomain
import com.herfy.database.entities.MenuItemEntity.Companion.toDomain
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.menu.MenuUtil
import com.skylinedynamics.solosdk.api.models.objects.Allergen
import com.skylinedynamics.solosdk.api.models.objects.AllergenCdn
import com.skylinedynamics.solosdk.api.models.objects.Ingredient
import com.skylinedynamics.solosdk.api.models.objects.IngredientCdn
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupCdn
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierItemCdn
import com.skylinedynamics.solosdk.util.GsonHelper
import java.util.LinkedList
import javax.inject.Inject


interface MenuRepository {

    suspend fun getMenuCategoriesItems(result: (List<MenuCategory>) -> Unit)

    suspend fun getMenuItem(menuItemId: String, result: (MenuItem) -> Unit)

}

class MenuRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val cacheUtilHelper: CacheUtilHelper
) : MenuRepository {

    override suspend fun getMenuCategoriesItems(result: (List<MenuCategory>) -> Unit) {
        cacheUtilHelper.getApplication()?.let { application ->
            val storeId  = CacheUtil.getOrderStoreId()
            val menuCategories = mutableListOf<MenuCategory>()
            val menuCategoryDbs = appDatabase.menuCategoryEntityDao().getAllByApplication(application.id)
            menuCategoryDbs.toDomain().forEach { menuCategory ->
                if(menuCategory.enabled == 1 && (storeId == null || !menuCategory.excludedLocations.contains(storeId))) {
                    val menuItemDbs = appDatabase.menuItemEntityDao().findAllByMenuCategoryId(application.id, menuCategory.id)
                    val filteredMenuItems = mutableListOf<MenuItem>()
                    menuItemDbs.toDomain().forEach { menuItem ->
                        if(menuItem.enabled == 1 &&
                            (storeId == null || menuItem.excludedLocations?.contains(storeId) == false)) {
                            filteredMenuItems.add(menuItem)
                        }
                    }
                    if(filteredMenuItems.isNotEmpty()) {
                        filteredMenuItems.sortBy { it.displayOrder }
                        menuCategory.menuItems = filteredMenuItems
                        menuCategories.add(menuCategory)
                    }
                }
            }
            menuCategories.sortBy { it.displayOrder }
            result(menuCategories)
        }
    }

    override suspend fun getMenuItem(menuItemId: String, result: (MenuItem) -> Unit) {
        cacheUtilHelper.getApplication()?.let { application ->
            val menuItemDb = appDatabase.menuItemDao().findByApplicationAndId(application.id, menuItemId)
            menuItemDb?.data?.let {
                val menuItem = MenuUtil.processMenuItemCustomObject(
                    GsonHelper.gsonFinal().fromJson(it, MenuItem::class.java)
                )
                val allergens = LinkedList<Allergen>()
                for (allergenDb in appDatabase.allergenDao().findAllByApplicationAndIds(
                    application.id,
                    menuItem.allergensIds.toTypedArray<String>()
                )) {
                    val allergenCdn = GsonHelper.gsonFinal().fromJson(
                        allergenDb.data,
                        AllergenCdn::class.java
                    )
                    allergens.add(
                        AllergenCdn.convert(
                            allergenCdn,
                            LocaleUtil.getInstance().language
                        )
                    )
                }
                allergens.sortWith { allergen1, allergen2 -> allergen1.attributes.displayOrder - allergen2.attributes.displayOrder }
                menuItem.allergens = allergens

                val ingredients = LinkedList<Ingredient>()
                val ingredientIds = HashMap<String, Ingredient>()
                for (ingredient in menuItem.ingredientIds) {
                    ingredientIds[ingredient.ingredientId] = ingredient
                }
                for (ingredientDb in appDatabase.ingredientDao().findAllByApplicationAndIds(
                    application.id,
                    ingredientIds.keys.toTypedArray<String>()
                )) {
                    val ingredientCdn = GsonHelper.gsonFinal().fromJson(
                        ingredientDb.data,
                        IngredientCdn::class.java
                    )
                    val ingredient =
                        IngredientCdn.convert(ingredientCdn, LocaleUtil.getInstance().language)
                    ingredient.id = ingredientIds[ingredientCdn.id]!!.id
                    ingredients.add(ingredient)
                }
                ingredients.sortWith { ingredient1, ingredient2 -> ingredient1.attributes.displayOrder - ingredient2.attributes.displayOrder }
                menuItem.ingredients = ingredients

                var sizes: ModifierGroup? = ModifierGroup()
                val modifierGroups = LinkedList<ModifierGroup>()
                val modifierGroupIds = HashMap<String, ModifierGroup>()
                for (modifierGroup in menuItem.modifierGroupIds) {
                    modifierGroupIds[modifierGroup.id] = modifierGroup
                }
                for (modifierGroupDb in appDatabase.modifierGroupDao().findAllByApplicationAndIds(
                    application.id,
                    modifierGroupIds.keys.toTypedArray<String>()
                )) {
                    val modifierGroupCdn = MenuUtil.processModifierGroupCustomObject(
                        GsonHelper.gsonFinal().fromJson(
                            modifierGroupDb.data,
                            ModifierGroupCdn::class.java
                        )
                    )
                    val modifierItems = LinkedList<ModifierItem>()
                    val modifierGroupModifierItems = modifierGroupIds[modifierGroupCdn.id]!!
                        .modifiers
                    val modifierGroupModifierItemIds = ArrayList<String>()
                    val excludeModifierItems = modifierGroupIds[modifierGroupCdn.id]!!
                        .excludeModifiers
                    val excludedModifierItems: MutableSet<String> = HashSet()
                    for (exclude in excludeModifierItems) {
                        excludedModifierItems.add(exclude.toString())
                    }

                    val modifierGroupModifierItemDisplayOrders = HashMap<String, Int?>()
                    for (modifierGroupModifierItem in modifierGroupModifierItems) {
                        if (!excludedModifierItems.contains(modifierGroupModifierItem.id)) {
                            modifierGroupModifierItemIds.add(modifierGroupModifierItem.id)
                            modifierGroupModifierItemDisplayOrders[modifierGroupModifierItem.id] =
                                modifierGroupModifierItems.indexOf(modifierGroupModifierItem)
                        }
                    }
                    for (modifierItemDb in appDatabase.modifierItemDao().findAllByApplicationAndIds(
                        application.id,
                        modifierGroupModifierItemIds.toTypedArray<String>()
                    )) {
                        val modifierItemCdn = GsonHelper.gsonFinal().fromJson(
                            modifierItemDb.data,
                            ModifierItemCdn::class.java
                        )
                        val modifierItem = ModifierItemCdn.convert(
                            modifierItemCdn,
                            LocaleUtil.getInstance().language
                        )
                        modifierItem.attributes.isSelected = modifierItem.attributes.minimum == 1
                        if (modifierGroupModifierItemDisplayOrders.containsKey(modifierItem.id) && modifierGroupModifierItemDisplayOrders[modifierItem.id] != null) {
                            modifierItem.attributes.displayOrder =
                                modifierGroupModifierItemDisplayOrders[modifierItem.id]!!
                        }
                        modifierItems.add(modifierItem)
                    }
                    modifierItems.sortWith { modifierItem1, modifierItem2 -> modifierItem1.attributes.displayOrder - modifierItem2.attributes.displayOrder }
                    val modifierGroup = ModifierGroupCdn.convert(
                        modifierGroupCdn,
                        LocaleUtil.getInstance().language
                    )
                    val mapModifierGroup = modifierGroupIds[modifierGroup.id]
                    if (mapModifierGroup != null) {
                        modifierGroup.attributes.minimum = mapModifierGroup.min
                        modifierGroup.attributes.maximum = mapModifierGroup.max
                        modifierGroup.attributes.displayOrder = mapModifierGroup.displayOrder
                        modifierGroup.attributes.freeModifiersCount =
                            mapModifierGroup.freeModifiersCount
                        modifierGroup.setDefaultModifiers(ArrayList(mapModifierGroup.defaultModifiers))
                        modifierGroup.attributes.visibilityConfig =
                            mapModifierGroup.visibilityConfig
                    }
                    modifierGroup.modifierItems = modifierItems

                    if (modifierGroupDb.type == ModifierGroupType.SIZES.value) {
                        sizes = modifierGroup
                    } else {
                        modifierGroups.add(modifierGroup)
                    }
                }
                menuItem.sizes = sizes
                modifierGroups.sortWith { modifierGroup1, modifierGroup2 -> modifierGroup1.attributes.displayOrder - modifierGroup2.attributes.displayOrder }

                menuItem.modifierGroups = modifierGroups

                val filteredModifierGroups = LinkedList<ModifierGroup>()

                for (modifierGroup in menuItem.modifierGroups) {
                    if (modifierGroup.modifierItems.size > 0) {
                        filteredModifierGroups.add(modifierGroup)
                    }
                }

                if (menuItem.sizes.modifierItems.size > 0) {
                    menuItem.sizes.modifierItems[0].attributes.isSelected = true
                }

                menuItem.modifierGroups = filteredModifierGroups

                for (modifierGroup in menuItem.modifierGroups) {
                    val defaultModifiers = modifierGroup.defaultModifiers
                    if (modifierGroup.modifierItems.size > 0 && modifierGroup.attributes.minimum > 0) {
                        if (defaultModifiers.size > 0) {
                            var selected = 0
                            for (i in modifierGroup.modifierItems.indices) {
                                val modifierItem = modifierGroup.modifierItems[i]
                                if (selected < modifierGroup.attributes.maximum && defaultModifiers.contains(
                                        modifierItem.id
                                    )
                                ) {
                                    modifierGroup.modifierItems[i].attributes.isSelected = true
                                    selected++
                                }
                            }
                        } else if (modifierGroup.attributes.minimum == 1) {
                            modifierGroup.modifierItems[0].attributes.isSelected = true
                        }
                    }
                }

                var visibilityConfig = menuItem.sizes.attributes.visibilityConfig
                if (visibilityConfig != null) {
                    val rules = visibilityConfig.rules
                    if (rules != null && rules.size > 0) {
                        val rule = rules[0]
                        if (rule.targetType != null && rule.targetType.equals(
                                "modifier-group",
                                ignoreCase = true
                            )
                        ) {
                            for (modGroup in menuItem.modifierGroups) {
                                if (menuItem.sizes.id != modGroup.id && rule.targetId == modGroup.id) {
                                    if (rule.targetIs != null && rule.targetIs.equals(
                                            "selected",
                                            ignoreCase = true
                                        )
                                    ) {
                                        var selected = 0
                                        for (modItem in modGroup.modifierItems) {
                                            if (modItem.attributes.isSelected) {
                                                selected += 1
                                            }
                                        }
                                        if (selected > 0) {
                                            menuItem.sizes.attributes.visibilityConfig.initialValue =
                                                ""
                                        } else {
                                            menuItem.sizes.attributes.visibilityConfig.initialValue =
                                                "hidden"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                for (group in menuItem.modifierGroups) {
                    visibilityConfig = group.attributes.visibilityConfig
                    if (visibilityConfig != null) {
                        val rules = visibilityConfig.rules
                        if (rules != null && rules.size > 0) {
                            val rule = rules[0]
                            if (rule.targetType != null) {
                                if (rule.targetType.equals("modifier-group", ignoreCase = true)) {
                                    if (group.id != menuItem.sizes.id && rule.targetId == menuItem.sizes.id) {
                                        if (rule.targetIs != null && rule.targetIs.equals(
                                                "selected",
                                                ignoreCase = true
                                            )
                                        ) {
                                            var selected = 0
                                            for (modItem in menuItem.sizes.modifierItems) {
                                                if (modItem.attributes.isSelected) {
                                                    selected += 1
                                                }
                                            }
                                            if (selected > 0) {
                                                group.attributes.visibilityConfig.initialValue = ""
                                            } else {
                                                group.attributes.visibilityConfig.initialValue =
                                                    "hidden"
                                            }
                                        }
                                    }
                                    for (modGroup in menuItem.modifierGroups) {
                                        if (group.id != modGroup.id && rule.targetId == modGroup.id) {
                                            if (rule.targetIs != null && rule.targetIs.equals(
                                                    "selected",
                                                    ignoreCase = true
                                                )
                                            ) {
                                                var selected = 0
                                                for (modItem in modGroup.modifierItems) {
                                                    if (modItem.attributes.isSelected) {
                                                        selected += 1
                                                    }
                                                }
                                                if (selected > 0) {
                                                    group.attributes.visibilityConfig.initialValue =
                                                        ""
                                                } else {
                                                    group.attributes.visibilityConfig.initialValue =
                                                        "hidden"
                                                }
                                            }
                                            break
                                        }
                                    }
                                } else if (rule.targetType.equals("modifier", ignoreCase = true)) {
                                    val selectedModifiers: MutableSet<String> = java.util.HashSet()
                                    for (modItem in menuItem.sizes.modifierItems) {
                                        if (modItem.attributes.isSelected) {
                                            selectedModifiers.add(modItem.id)
                                            break
                                        }
                                    }
                                    for (modGroup in menuItem.modifierGroups) {
                                        if (group.id != modGroup.id) {
                                            for (modItem in modGroup.modifierItems) {
                                                if (modItem.attributes.isSelected) {
                                                    selectedModifiers.add(modItem.id)
                                                }
                                            }
                                        }
                                    }

                                    if (rule.targetId != null && selectedModifiers.contains(rule.targetId)) {
                                        if (rule.targetIs != null && rule.targetIs.equals(
                                                "selected",
                                                ignoreCase = true
                                            )
                                        ) {
                                            group.attributes.visibilityConfig.initialValue = ""
                                        } else {
                                            group.attributes.visibilityConfig.initialValue =
                                                "hidden"
                                        }
                                    } else if (rule.targetId.isNotEmpty()) {
                                        group.attributes.visibilityConfig.initialValue = "hidden"
                                    }
                                }
                            }
                        }
                    }
                }

                result(menuItem)

            }
        }
    }

}