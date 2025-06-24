package com.emfad.app.database.converters

import androidx.room.TypeConverter
import com.emfad.app.models.MaterialType
import com.emfad.app.models.SessionStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room Type Converters für EMFAD Database
 * Samsung S21 Ultra optimiert
 */
class EMFADTypeConverters {
    
    private val gson = Gson()
    
    // MaterialType Converters
    @TypeConverter
    fun fromMaterialType(materialType: MaterialType): String {
        return materialType.name
    }
    
    @TypeConverter
    fun toMaterialType(materialType: String): MaterialType {
        return try {
            MaterialType.valueOf(materialType)
        } catch (e: IllegalArgumentException) {
            MaterialType.UNKNOWN
        }
    }
    
    // SessionStatus Converters
    @TypeConverter
    fun fromSessionStatus(status: SessionStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toSessionStatus(status: String): SessionStatus {
        return try {
            SessionStatus.valueOf(status)
        } catch (e: IllegalArgumentException) {
            SessionStatus.CREATED
        }
    }
    
    // List<String> Converters
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // List<Double> Converters
    @TypeConverter
    fun fromDoubleList(value: List<Double>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toDoubleList(value: String): List<Double> {
        return try {
            val listType = object : TypeToken<List<Double>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // List<Int> Converters
    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toIntList(value: String): List<Int> {
        return try {
            val listType = object : TypeToken<List<Int>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Map<String, Any> Converters
    @TypeConverter
    fun fromStringAnyMap(value: Map<String, Any>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringAnyMap(value: String): Map<String, Any> {
        return try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson(value, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    // Map<String, Double> Converters
    @TypeConverter
    fun fromStringDoubleMap(value: Map<String, Double>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringDoubleMap(value: String): Map<String, Double> {
        return try {
            val mapType = object : TypeToken<Map<String, Double>>() {}.type
            gson.fromJson(value, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    // Array<Float> Converters (für AR/3D Daten)
    @TypeConverter
    fun fromFloatArray(value: Array<Float>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toFloatArray(value: String): Array<Float> {
        return try {
            val arrayType = object : TypeToken<Array<Float>>() {}.type
            gson.fromJson(value, arrayType) ?: emptyArray()
        } catch (e: Exception) {
            emptyArray()
        }
    }
    
    // ByteArray Converters (für Rohdaten)
    @TypeConverter
    fun fromByteArray(value: ByteArray): String {
        return android.util.Base64.encodeToString(value, android.util.Base64.DEFAULT)
    }
    
    @TypeConverter
    fun toByteArray(value: String): ByteArray {
        return try {
            android.util.Base64.decode(value, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            byteArrayOf()
        }
    }
    
    // Long? (Nullable Long) Converters
    @TypeConverter
    fun fromNullableLong(value: Long?): Long {
        return value ?: -1L
    }
    
    @TypeConverter
    fun toNullableLong(value: Long): Long? {
        return if (value == -1L) null else value
    }
    
    // Complex Number Converters (für EMF-Daten)
    @TypeConverter
    fun fromComplexNumber(value: Pair<Double, Double>): String {
        return "${value.first},${value.second}"
    }
    
    @TypeConverter
    fun toComplexNumber(value: String): Pair<Double, Double> {
        return try {
            val parts = value.split(",")
            if (parts.size == 2) {
                Pair(parts[0].toDouble(), parts[1].toDouble())
            } else {
                Pair(0.0, 0.0)
            }
        } catch (e: Exception) {
            Pair(0.0, 0.0)
        }
    }
    
    // 3D Vector Converters (für AR-Positionen)
    @TypeConverter
    fun fromVector3D(value: Triple<Double, Double, Double>): String {
        return "${value.first},${value.second},${value.third}"
    }
    
    @TypeConverter
    fun toVector3D(value: String): Triple<Double, Double, Double> {
        return try {
            val parts = value.split(",")
            if (parts.size == 3) {
                Triple(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble())
            } else {
                Triple(0.0, 0.0, 0.0)
            }
        } catch (e: Exception) {
            Triple(0.0, 0.0, 0.0)
        }
    }
    
    // JSON Generic Converter für komplexe Objekte
    @TypeConverter
    fun fromJsonString(value: Any?): String {
        return if (value == null) "" else gson.toJson(value)
    }
    
    inline fun <reified T> toJsonObject(value: String): T? {
        return try {
            if (value.isEmpty()) null else gson.fromJson(value, T::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
