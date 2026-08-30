package com.example.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class SmsHistoryStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sms_sender_prefs", Context.MODE_PRIVATE)

    fun loadHistory(): List<SmsHistoryItem> {
        val jsonString = prefs.getString("history_items", null) ?: return emptyList()
        val list = mutableListOf<SmsHistoryItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SmsHistoryItem(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        firstName = obj.optString("firstName", ""),
                        lastName = obj.optString("lastName", ""),
                        phoneNumber = obj.optString("phoneNumber", ""),
                        fullMessage = obj.optString("fullMessage", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isDirectSms = obj.optBoolean("isDirectSms", true),
                        isSuccess = obj.optBoolean("isSuccess", true)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveHistory(items: List<SmsHistoryItem>) {
        try {
            val jsonArray = JSONArray()
            for (item in items) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("firstName", item.firstName)
                obj.put("lastName", item.lastName)
                obj.put("phoneNumber", item.phoneNumber)
                obj.put("fullMessage", item.fullMessage)
                obj.put("timestamp", item.timestamp)
                obj.put("isDirectSms", item.isDirectSms)
                obj.put("isSuccess", item.isSuccess)
                jsonArray.put(obj)
            }
            prefs.edit().putString("history_items", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
