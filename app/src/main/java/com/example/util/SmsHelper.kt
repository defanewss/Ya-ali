package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast

object SmsHelper {

    fun sendDirectSms(
        context: Context,
        phoneNumber: String,
        message: String
    ): Result<Unit> {
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val cleanPhone = phoneNumber.trim()
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(cleanPhone, null, message, null, null)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openInSmsApp(
        context: Context,
        phoneNumber: String,
        message: String
    ): Boolean {
        return try {
            val cleanPhone = phoneNumber.trim()
            val uri = Uri.parse("smsto:${Uri.encode(cleanPhone)}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback to ACTION_VIEW
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:${Uri.encode(phoneNumber.trim())}")
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (ex: Exception) {
                Toast.makeText(context, "برنامه پیام‌رسانی یافت نشد", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }
}
