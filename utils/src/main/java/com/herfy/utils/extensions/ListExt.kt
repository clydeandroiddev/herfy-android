package com.herfy.utils.extensions

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylinedynamics.solosdk.util.GsonHelper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.reflect.KClass


fun <T, U> List<T>.intersect(uList: List<U>, filterPredicate : (T, U) -> Boolean) = filter { m -> uList.any { filterPredicate(m, it)} }

fun <T, U> List<T>.contains(o: U, filterPredicate: (T, U) -> Boolean) = find { m -> any { filterPredicate(m, o) }}

fun <T> T.cloneList() : T
{
    val byteArrayOutputStream= ByteArrayOutputStream()
    ObjectOutputStream(byteArrayOutputStream).use { outputStream ->
        outputStream.writeObject(this)
    }

    val bytes=byteArrayOutputStream.toByteArray()

    ObjectInputStream(ByteArrayInputStream(bytes)).use { inputStream ->
        return inputStream.readObject() as T
    }
}



class NotADataClassException(clazz: KClass<out Any>) : Exception("$clazz is not a data class")

fun <T : Any> List<T>.deepCopy(clazz: Class<T>): ArrayList<T> {
    val listString = GsonHelper.gsonFinal().toJson(this)
    val listType = TypeToken.getParameterized(ArrayList::class.java, clazz).type
    val gson = Gson()
    val list : ArrayList<T> = gson.fromJson(listString, listType)
    return list
}