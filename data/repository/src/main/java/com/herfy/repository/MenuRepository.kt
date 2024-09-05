package com.herfy.repository

import com.google.gson.reflect.TypeToken
import com.herfy.database.AppDatabase
import com.herfy.database.entities.AllergenDb
import com.herfy.database.entities.IngredientDb
import com.herfy.database.entities.MenuCategoryEntity.Companion.toDomain
import com.herfy.database.entities.MenuItemDb
import com.herfy.database.entities.MenuItemEntity.Companion.toDomain
import com.herfy.database.entities.ModifierGroupDb
import com.herfy.database.entities.ModifierItemDb
import com.herfy.database.responses.MenuCategoryItemResponse
import com.herfy.utils.extensions.cast
import com.herfy.utils.helper.AppLogger
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.menu.MenuUtil
import com.skylinedynamics.solosdk.api.ApiRequestWithHeadersListener
import com.skylinedynamics.solosdk.api.calls.UtilitiesApiCall
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.calls.MenuApiCall
import com.skylinedynamics.solosdk.api.models.Pagination
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
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.requestbodies.AddFavoriteItemBody
import com.skylinedynamics.solosdk.util.GsonHelper
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import javax.inject.Inject


interface MenuRepository {

    suspend fun getMenu(onComplete: () -> Unit)

    suspend fun getMenuCategoriesItems(result: (List<MenuCategory>) -> Unit)

    suspend fun getMenuItem(menuItemId: String, result: (MenuItem) -> Unit)
    suspend fun getMenuCategoryIdFromMenuItemEntityBy(id: String, applicationId: String, result: (String) -> Unit)
    suspend fun getMenuItemDbBy(applicationId: String, name: String, result: (MenuItemDb?) -> Unit)
    fun getAllModifierGroupBy(
        applicationId: String,
        toTypedArray: Array<String>,
        result: (List<ModifierGroupDb>) -> Unit
    )
    fun getModifierItemBy(applicationId: String, toTypedArray: Array<String>, result: (List<ModifierItemDb>) -> Unit)

    suspend fun getFrequentlyBoughtItems(page : Int, menuId: String, result: (ArrayList<MenuItem>, Int ) -> Unit)

    suspend fun getMenuCategory(menuCategoryId : String, applicationId : String, result: (MenuCategory) -> Unit)

    suspend fun addItemToFavorite(menuItem : MenuItem, result: (Boolean, Any?) -> Unit)

    suspend fun deleteItemFromFavorites(menuItemId : String, result: (Boolean, Any?) -> Unit)

    suspend fun getMenuItemDbById(id: String, applicationId: String, result: (MenuItemDb?) -> Unit)
}

class MenuRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val cacheUtilHelper: CacheUtilHelper
) : MenuRepository {

    override suspend fun getMenu( onComplete: () -> Unit) {
        cacheUtilHelper.getApplication()?.let { application ->
            val applicationId =  application.id
            appDatabase.menuCategoryDao().deleteByApplication(applicationId)
            appDatabase.menuItemDao().deleteByApplication(applicationId)
            appDatabase.modifierGroupDao().deleteByApplication(applicationId)
            appDatabase.modifierItemDao().deleteByApplication(applicationId)
            appDatabase.ingredientDao().deleteByApplication(applicationId)
            appDatabase.allergenDao().deleteByApplication(applicationId)
            appDatabase.menuCategoryEntityDao().deleteByApplication(applicationId)
            appDatabase.menuItemEntityDao().deleteByApplication(applicationId)

            val menuUrl = MenuUtil.getDiffMenuCdnUrl()

            UtilitiesApiCall.getInstance()
                .getMenuCategoriesWithHeaders(menuUrl, object : ApiRequestWithHeadersListener {
                    override fun onSuccess(headers: Headers, o: Any) {
                        try {
                            if (headers.names().contains("last-modified")) {
                                CacheUtil.getInstance().menuCdnLastModified = headers["last-modified"]
                            }
                            val data = JSONObject(o.toString())
                            try {
                                val dataArray = data.getJSONArray("data")
                                val menuCategoryItemsResponse = GsonHelper.gsonFinal()
                                    .fromJson<List<MenuCategoryItemResponse>>(
                                        dataArray.toString(),
                                        object :
                                            TypeToken<java.util.ArrayList<MenuCategoryItemResponse?>?>() {}.type
                                    )
                                val included = data.getJSONObject("included")
                                var allergensArray = JSONArray()
                                if (included.has("allergens") && !included.isNull("allergens")) {
                                    allergensArray = included.getJSONArray("allergens")
                                }
                                for (index in 0 until allergensArray.length()) {
                                    val allergenObject = allergensArray.getJSONObject(index)
                                    val allergenId = allergenObject.getLong("id").toString()
                                    val allergenDb = AllergenDb()
                                    allergenDb.applicationId = applicationId
                                    allergenDb.itemAllergenId = allergenId
                                    allergenDb.id = allergenId
                                    allergenDb.data = allergenObject.toString()
                                    appDatabase.allergenDao().insertAll(allergenDb)
                                }
                                var ingredientsArray = JSONArray()
                                if (included.has("ingredients") && !included.isNull("ingredients")) {
                                    ingredientsArray = included.getJSONArray("ingredients")
                                }
                                for (index in 0 until ingredientsArray.length()) {
                                    val ingredientObject = ingredientsArray.getJSONObject(index)
                                    val ingredientId = ingredientObject.getLong("id").toString()
                                    val ingredientDb = IngredientDb()
                                    ingredientDb.applicationId = applicationId
                                    ingredientDb.itemIngredientId = ingredientId
                                    ingredientDb.id = ingredientId
                                    ingredientDb.data = ingredientObject.toString()
                                    appDatabase.ingredientDao().insertAll(ingredientDb)
                                }
                                var modifierGroupsArray = JSONArray()
                                if (included.has("modifierGroups") && !included.isNull("modifierGroups")) {
                                    modifierGroupsArray = included.getJSONArray("modifierGroups")
                                }
                                for (index in 0 until modifierGroupsArray.length()) {
                                    val modifierGroupObject = modifierGroupsArray.getJSONObject(index)
                                    val modifierGroupAttributesObject =
                                        modifierGroupObject.getJSONObject("attributes")
                                    var code = ""
                                    if (modifierGroupAttributesObject.getString("code") != null) {
                                        code = modifierGroupAttributesObject.getString("code").trim { it <= ' ' }
                                            .lowercase(Locale.getDefault())
                                    }
                                    var type = ""
                                    if (modifierGroupAttributesObject.getString("type") != null) {
                                        type = modifierGroupAttributesObject.getString("type").trim { it <= ' ' }
                                            .lowercase(Locale.getDefault())
                                    }
                                    var label = ""
                                    if (modifierGroupAttributesObject.getString("label") != null) {
                                        label = modifierGroupAttributesObject.getString("label").trim { it <= ' ' }
                                            .lowercase(Locale.getDefault())
                                    }
                                    var modifierGroupType = ModifierGroupType.OPTIONS
                                    if (code == "sizes" || type == "sizes" || label == "sizes") {
                                        modifierGroupType = ModifierGroupType.SIZES
                                    }
                                    val modifierGroupDb = ModifierGroupDb()
                                    val modifierGroupId = modifierGroupObject.getLong("id").toString()
                                    modifierGroupDb.itemGroupId = modifierGroupId
                                    modifierGroupDb.id = modifierGroupId
                                    modifierGroupDb.applicationId = applicationId
                                    modifierGroupDb.type = modifierGroupType.value
                                    modifierGroupDb.data = modifierGroupObject.toString()
                                    appDatabase.modifierGroupDao().insertAll(modifierGroupDb)
                                }
                                var modifiersArray = JSONArray()
                                if (included.has("modifiers") && !included.isNull("modifiers")) {
                                    modifiersArray = included.getJSONArray("modifiers")
                                }
                                for (index in 0 until modifiersArray.length()) {
                                    val modifierItemObject = modifiersArray.getJSONObject(index)
                                    val modifierItemId = modifierItemObject.getLong("id").toString()
                                    val modifierItemDb = ModifierItemDb()
                                    modifierItemDb.applicationId = applicationId
                                    modifierItemDb.itemGroupModifierId = modifierItemId
                                    modifierItemDb.id = modifierItemId
                                    modifierItemDb.data = modifierItemObject.toString()
                                    appDatabase.modifierItemDao().insertAll(modifierItemDb)
                                }
                                val menuItemDbs = java.util.ArrayList<MenuItemDb>()
                                for (index in 0 until dataArray.length()) {
                                    val menuCategoryItemObject = dataArray.getJSONObject(index)
                                    val menuCategoryItemAttributesObject =
                                        menuCategoryItemObject.getJSONObject("attributes")
                                    val menuCategoryObject =
                                        menuCategoryItemAttributesObject.getJSONObject("category")
                                    val menuItemsArray = menuCategoryItemAttributesObject.getJSONArray("items")
                                    for (menuItemIndex in 0 until menuItemsArray.length()) {
                                        val menuItemObject = menuItemsArray.getJSONObject(menuItemIndex)
                                        val menuItemDb = MenuItemDb()
                                        val menuItemId = menuItemObject.getLong("id").toString()
                                        menuItemDb.id = menuItemId
                                        menuItemDb.applicationId = applicationId
                                        menuItemDb.name = menuItemObject.getJSONObject("name")
                                            .getString(LocaleUtil.getInstance().language)
                                        menuItemDb.menuCategoryId = menuCategoryObject.getLong("id").toString()
                                        val menuItemDbData = menuItemObject.toString()
                                        menuItemDb.data = menuItemDbData
                                        menuItemDbs.add(menuItemDb)
                                    }
                                }
                                try {
                                    appDatabase.menuItemDao().insertAll(*menuItemDbs.toTypedArray<MenuItemDb>())
                                } catch (e: ArrayIndexOutOfBoundsException) {
                                    e.printStackTrace()
                                }
                                for (menuCategoryItem in menuCategoryItemsResponse) {
                                    val menuCategoryEntity = menuCategoryItem.attributes.menuCategory
                                    menuCategoryEntity.applicationId = applicationId
                                    if (!MenuUtil.isExcluded(menuCategoryEntity.excludedApps)) {
                                        appDatabase.menuCategoryEntityDao().insertAll(menuCategoryEntity)
                                        for (menuItem in menuCategoryItem.attributes.menuItems) {
                                            menuItem.applicationId = applicationId
                                            menuItem.menuCategoryId = menuCategoryItem.attributes.menuCategory.id
                                            if (!MenuUtil.isExcluded(menuItem.excludedApps)) {
                                                appDatabase.menuItemEntityDao().insertAll(menuItem)
                                            }
                                        }
                                    }
                                }
                                CacheUtil.getInstance().menuCached = Date().time
                                onComplete()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onError(o: Any) {
                        AppLogger.printRetrofitError(o.toString())
                    }
                })
        }
    }

    override suspend fun getMenuCategoriesItems(result: (List<MenuCategory>) -> Unit) {
        cacheUtilHelper.getApplication()?.let { application ->
            val storeId = CacheUtil.getOrderStoreId()
            val menuCategories = mutableListOf<MenuCategory>()
            val menuCategoryDbs =
                appDatabase.menuCategoryEntityDao().getAllByApplication(application.id)
            menuCategoryDbs.toDomain().forEach { menuCategory ->
                val isHiddenFromMenuKey = "is_hidden_from_menu"

                var isCategoryHiddenFromMenu = false
                if(menuCategory.customFieldsMap.containsKey(isHiddenFromMenuKey)){
                    menuCategory.customFieldsMap.get(isHiddenFromMenuKey)?.cast<String> {
                        isCategoryHiddenFromMenu = it.equals("1", true)
                    }
                }
                if (menuCategory.enabled == 1 && !isCategoryHiddenFromMenu &&
                    (storeId == null ||
                            (!menuCategory.excludedApps.contains(application.id) &&
                                    !menuCategory.excludedLocations.contains(storeId))
                            )
                ) {
                    val menuItemDbs = appDatabase.menuItemEntityDao()
                        .findAllByMenuCategoryId(application.id, menuCategory.id)
                    val filteredMenuItems = mutableListOf<MenuItem>()
                    menuItemDbs.toDomain().forEach { menuItem ->
                        var isMenuItemHiddenFromMenu = false
                        if(menuItem.customFieldsMap.containsKey(isHiddenFromMenuKey)){
                            menuItem.customFieldsMap.get(isHiddenFromMenuKey)?.cast<String> {
                                isMenuItemHiddenFromMenu = it.equals("1", true)
                            }
                        }

                        if(!isMenuItemHiddenFromMenu) {
                            val orderType = CacheUtil.getInstance().orderType
                            if (orderType == OrderType.DELIVERY) {
                                isMenuItemHiddenFromMenu = menuItem.disableForDelivery == 1
                            }
                        }

                        if(!isMenuItemHiddenFromMenu) {
                            val pickupOrderTypes =
                                listOf(OrderType.PICKUP, OrderType.DRIVE_THRU, OrderType.CURBSIDE)
                            val orderType = CacheUtil.getInstance().orderType
                            if (orderType in pickupOrderTypes) {
                                isMenuItemHiddenFromMenu = menuItem.disableForPickup == 1
                            }
                        }

                        if (menuItem.enabled == 1 && !isMenuItemHiddenFromMenu &&
                            (storeId == null ||
                                    (menuItem.excludedApps?.contains(application.id) == false &&
                                            menuItem.excludedLocations?.contains(storeId) == false)
                                    )
                        ) {
                            filteredMenuItems.add(menuItem)
                        }
                    }
                    if (filteredMenuItems.isNotEmpty()) {
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
            val menuItemDb =
                appDatabase.menuItemDao().findByApplicationAndId(application.id, menuItemId)
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
                        modifierGroup.modifierItems.map { item -> item.attributes.isSelected = false }
                        modifierGroup.modifierItems[0].attributes.isSelected = true
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

    override suspend fun getMenuCategoryIdFromMenuItemEntityBy(
        id: String,
        applicationId: String,
        result: (String) -> Unit
    ) {
        val menuCategoryId = appDatabase.menuItemEntityDao().findById(applicationId, id).menuCategoryId
        result(menuCategoryId)
    }

    override suspend fun getMenuItemDbBy(
        applicationId: String,
        name: String,
        result: (MenuItemDb?) -> Unit
    ) {
        result(appDatabase.menuItemDao().findByApplicationAndName(applicationId, name.trim()))
    }

    override fun getAllModifierGroupBy(
        applicationId: String,
        ids: Array<String>,
        result: (List<ModifierGroupDb>) -> Unit
    ) {
        result(
            appDatabase.modifierGroupDao()
                .findAllByApplicationAndIds(
                    applicationId,
                    ids
                )
        )
    }

    override fun getModifierItemBy(
        applicationId: String,
        ids: Array<String>,
        result: (List<ModifierItemDb>) -> Unit
    ) {
        result(appDatabase.modifierItemDao()
            .findAllByApplicationAndIds(
                applicationId,
                ids
            ))
    }

    override suspend fun getFrequentlyBoughtItems(page : Int, menuId : String, result: (ArrayList<MenuItem>, Int) -> Unit) {
        MenuApiCall.getInstance().getFrequentlyBoughtMenuItems(CacheUtil.getInstance().menuId, menuId, page.toString(), object : ApiRequestListener{
            override fun onSuccess(obj: Any?) {
               try {
                   val list = arrayListOf<MenuItem>()
                   val `object` = JSONObject(obj.toString())
                   val pagination = Pagination.parse(`object`)
                   val response = `object`.getJSONArray("data")
                   for (i in 0 until response.length()) {
                       val jsonObject = response.getJSONObject(i)
                       val frequentBoughtMenu = GsonHelper.gsonFinal().fromJson(
                           jsonObject.toString(),
                           MenuItem::class.java
                       )
                       list.add(frequentBoughtMenu)
                   }

                   result(list, pagination.totalPages)
               }catch(e:Exception) {
                   e.printStackTrace()
                   this.onError(e)
               }
            }

            override fun onError(error: Any?) {
                AppLogger.printRetrofitError(error)
            }

        })
    }

    override suspend fun getMenuCategory(
        menuCategoryId: String,
        applicationId: String,
        result: (MenuCategory) -> Unit
    ) {
        val menuCategoryDb = appDatabase.menuCategoryEntityDao().findById(applicationId, menuCategoryId)
        val menuCategory = menuCategoryDb.toDomain()
        result(menuCategory)
    }

    override suspend fun addItemToFavorite(menuItem: MenuItem, result: (Boolean, Any?) -> Unit) {
        val customerId = AuthUtil.getInstance().customer.id
        val addFavoriteItemBody = AddFavoriteItemBody.getCdn(LocaleUtil.getInstance().language, menuItem)
        CustomersApiCall.getInstance().addItemToFavorite(customerId, addFavoriteItemBody, object : ApiRequestListener{
            override fun onSuccess(obj: Any?) {
                result(true, null)
            }

            override fun onError(error: Any?) {
                result(true, error)
            }

        })
    }

    override suspend fun deleteItemFromFavorites(
        menuItemId: String,
        result: (Boolean, Any?) -> Unit
    ) {
        val customerId = AuthUtil.getInstance().customer.id
        CustomersApiCall.getInstance().deleteItemFromFavorites(customerId, menuItemId, object : ApiRequestListener{
            override fun onSuccess(obj: Any?) {
                result(true, null)
            }

            override fun onError(error: Any?) {
               result(false, error)
            }

        })
    }

    override suspend fun getMenuItemDbById(
        id: String,
        applicationId: String,
        result: (MenuItemDb?) -> Unit
    ) {
        result(appDatabase.menuItemDao().findByApplicationAndId(applicationId, id))
    }


}