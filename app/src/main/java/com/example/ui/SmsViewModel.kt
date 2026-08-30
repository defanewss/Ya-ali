package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SmsHistoryItem
import com.example.data.SmsHistoryStorage
import com.example.util.SmsHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class SmsTemplate(val title: String, val pattern: String) {
    STANDARD("استاندارد", "نام و نام خانوادگی: {name}"),
    NAME_ONLY("فقط نام", "{name}"),
    FORMAL("معرفی رسمی", "با سلام، مشخصات اینجانب: {name}"),
    REGISTRATION("ثبت اطلاعات", "جهت ثبت‌نام:\n{name}")
}

data class SmsUiState(
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val selectedTemplate: SmsTemplate = SmsTemplate.STANDARD,
    val isSending: Boolean = false,
    val history: List<SmsHistoryItem> = emptyList(),
    val showConfirmDialog: Boolean = false
) {
    val fullName: String
        get() = "${firstName.trim()} ${lastName.trim()}".trim()

    val formattedMessage: String
        get() {
            val name = if (fullName.isBlank()) "..." else fullName
            return selectedTemplate.pattern.replace("{name}", name)
        }

    val isValid: Boolean
        get() = firstName.isNotBlank() && lastName.isNotBlank() && phoneNumber.isNotBlank()
}

class SmsViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = SmsHistoryStorage(application)

    private val _uiState = MutableStateFlow(SmsUiState())
    val uiState: StateFlow<SmsUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val list = storage.loadHistory()
        _uiState.update { it.copy(history = list) }
    }

    fun onFirstNameChanged(name: String) {
        _uiState.update { it.copy(firstName = name) }
    }

    fun onLastNameChanged(lastName: String) {
        _uiState.update { it.copy(lastName = lastName) }
    }

    fun onPhoneNumberChanged(phone: String) {
        // Filter valid phone characters (+, digits, spaces)
        val filtered = phone.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' }
        _uiState.update { it.copy(phoneNumber = filtered) }
    }

    fun onTemplateSelected(template: SmsTemplate) {
        _uiState.update { it.copy(selectedTemplate = template) }
    }

    fun clearInputs() {
        _uiState.update {
            it.copy(
                firstName = "",
                lastName = "",
                phoneNumber = ""
            )
        }
    }

    fun populateFromHistory(item: SmsHistoryItem) {
        _uiState.update {
            it.copy(
                firstName = item.firstName,
                lastName = item.lastName,
                phoneNumber = item.phoneNumber
            )
        }
        viewModelScope.launch {
            _snackbarEvents.emit("اطلاعات پیامک قبلی بارگذاری شد")
        }
    }

    fun sendDirectSms(context: Context) {
        val state = _uiState.value
        if (!state.isValid) {
            viewModelScope.launch {
                _snackbarEvents.emit("لطفاً تمامی فیلدها (نام، نام خانوادگی، شماره) را کامل کنید")
            }
            return
        }

        _uiState.update { it.copy(isSending = true) }
        val message = state.formattedMessage
        val result = SmsHelper.sendDirectSms(context, state.phoneNumber, message)

        _uiState.update { it.copy(isSending = false) }

        val isSuccess = result.isSuccess
        val historyItem = SmsHistoryItem(
            id = UUID.randomUUID().toString(),
            firstName = state.firstName.trim(),
            lastName = state.lastName.trim(),
            phoneNumber = state.phoneNumber.trim(),
            fullMessage = message,
            timestamp = System.currentTimeMillis(),
            isDirectSms = true,
            isSuccess = isSuccess
        )

        val updatedHistory = listOf(historyItem) + _uiState.value.history
        storage.saveHistory(updatedHistory)
        _uiState.update { it.copy(history = updatedHistory) }

        viewModelScope.launch {
            if (isSuccess) {
                _snackbarEvents.emit("پیامک به شماره ${state.phoneNumber} ارسال شد")
            } else {
                _snackbarEvents.emit("خطا در ارسال پیامک: ${result.exceptionOrNull()?.localizedMessage ?: "عدم دسترسی"}")
            }
        }
    }

    fun openInSmsApp(context: Context) {
        val state = _uiState.value
        if (!state.isValid) {
            viewModelScope.launch {
                _snackbarEvents.emit("لطفاً تمامی فیلدها را کامل کنید")
            }
            return
        }

        val message = state.formattedMessage
        val opened = SmsHelper.openInSmsApp(context, state.phoneNumber, message)
        if (opened) {
            val historyItem = SmsHistoryItem(
                id = UUID.randomUUID().toString(),
                firstName = state.firstName.trim(),
                lastName = state.lastName.trim(),
                phoneNumber = state.phoneNumber.trim(),
                fullMessage = message,
                timestamp = System.currentTimeMillis(),
                isDirectSms = false,
                isSuccess = true
            )
            val updatedHistory = listOf(historyItem) + _uiState.value.history
            storage.saveHistory(updatedHistory)
            _uiState.update { it.copy(history = updatedHistory) }

            viewModelScope.launch {
                _snackbarEvents.emit("برنامه پیام‌رسانی باز شد")
            }
        }
    }

    fun deleteHistoryItem(id: String) {
        val updated = _uiState.value.history.filter { it.id != id }
        storage.saveHistory(updated)
        _uiState.update { it.copy(history = updated) }
    }

    fun clearAllHistory() {
        storage.saveHistory(emptyList())
        _uiState.update { it.copy(history = emptyList()) }
        viewModelScope.launch {
            _snackbarEvents.emit("تاریخچه پاکسازی شد")
        }
    }
}
