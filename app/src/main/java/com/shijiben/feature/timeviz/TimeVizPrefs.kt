package com.shijiben.feature.timeviz

import android.content.SharedPreferences

/**
 * 时间可视化偏好存储。生日与寿命，纯本地 SharedPreferences，不进 Room。
 */
interface TimeVizPrefs {
    /** 生日当地 00:00 millis，0 表示未设 */
    fun getBirthdayMillis(): Long

    /** 设置生日 millis，0 清除 */
    fun setBirthdayMillis(millis: Long)

    /** 假设寿命，默认 80 */
    fun getLifespanYears(): Int

    /** 设置寿命 */
    fun setLifespanYears(years: Int)
}

private const val KEY_BIRTHDAY = "birthday_millis"
private const val KEY_LIFESPAN = "lifespan_years"
private const val DEFAULT_LIFESPAN = 80
private const val DEFAULT_BIRTHDAY = 0L

/**
 * SharedPreferences-backed 实现。getLifespanYears 在 key 不存在时返回 80；
 * getBirthdayMillis 在 key 不存在时返回 0L。
 */
private class TimeVizPrefsImpl(
    private val prefs: SharedPreferences
) : TimeVizPrefs {

    override fun getBirthdayMillis(): Long =
        prefs.getLong(KEY_BIRTHDAY, DEFAULT_BIRTHDAY)

    override fun setBirthdayMillis(millis: Long) {
        prefs.edit().putLong(KEY_BIRTHDAY, millis).apply()
    }

    override fun getLifespanYears(): Int =
        prefs.getInt(KEY_LIFESPAN, DEFAULT_LIFESPAN)

    override fun setLifespanYears(years: Int) {
        prefs.edit().putInt(KEY_LIFESPAN, years).apply()
    }
}

/** 工厂函数：让 Hilt 模块可在不直接引用 private 类的情况下创建实例。 */
internal fun createTimeVizPrefs(prefs: SharedPreferences): TimeVizPrefs =
    TimeVizPrefsImpl(prefs)
