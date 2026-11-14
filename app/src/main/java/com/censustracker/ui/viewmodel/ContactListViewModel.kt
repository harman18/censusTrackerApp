package com.censustracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.censustracker.contacts.DeviceContact
import com.censustracker.contacts.DeviceContactReader
import com.censustracker.contacts.PhoneContactWriter
import com.censustracker.data.json.JsonBackupService
import com.censustracker.domain.model.Contact
import com.censustracker.domain.model.ContactActivity
import com.censustracker.domain.model.Message
import com.censustracker.domain.usecase.AddContactUseCase
import com.censustracker.domain.usecase.DeleteContactUseCase
import com.censustracker.domain.usecase.GetAllContactsUseCase
import com.censustracker.domain.usecase.UpdateContactUseCase
import com.censustracker.domain.usecase.WatchAllMessagesUseCase
import com.censustracker.logging.ErrorLogger
import com.censustracker.preferences.AppPreferences
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ContactFilter {
    ALL, SAVED_TO_PHONE, NOT_SAVED
}

enum class ContactSort {
    NAME, LAST_ACTIVITY
}

@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val getAllContactsUseCase: GetAllContactsUseCase,
    private val addContactUseCase: AddContactUseCase,
    private val updateContactUseCase: UpdateContactUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
    private val deviceContactReader: DeviceContactReader,
    private val phoneContactWriter: PhoneContactWriter,
    private val jsonBackupService: JsonBackupService,
    private val errorLogger: ErrorLogger,
    private val appPreferences: AppPreferences,
    private val watchAllMessagesUseCase: WatchAllMessagesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val allContacts: List<Contact> = emptyList(),
        val filteredContacts: List<Contact> = emptyList(),
        val messageSummaries: Map<Long, ContactActivity> = emptyMap(),
        val deviceContacts: List<DeviceContact> = emptyList(),
        val showImportDialog: Boolean = false,
        val searchQuery: String = "",
        val selectedFilter: ContactFilter = ContactFilter.ALL,
        val sortBy: ContactSort = ContactSort.NAME,
        val isLoading: Boolean = false,
        val isProcessingAction: Boolean = false,
        val processingTitle: String = "Processing...",
        val error: String? = null,
        val infoMessage: String? = null,
        val toastSuccess: String? = null,
        val toastError: String? = null
    )

    init {
        val savedSort = if (appPreferences.contactSort == "LAST_ACTIVITY") {
            ContactSort.LAST_ACTIVITY
        } else {
            ContactSort.NAME
        }
        _uiState.update { it.copy(sortBy = savedSort) }
        observeContactsInRealTime()
    }

    private fun observeContactsInRealTime() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            combine(
                getAllContactsUseCase.watch(),
                watchAllMessagesUseCase.watchAll()
            ) { contacts, messages -> contacts to messages }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = "Database observation error: ${e.message}") }
                }
                .collect { (contacts, messages) ->
                    val summaries = buildActivitySummaries(messages)
                    _uiState.update { current ->
                        val filtered = applyFilter(
                            contacts,
                            current.searchQuery,
                            current.selectedFilter,
                            current.sortBy,
                            summaries
                        )
                        current.copy(
                            allContacts = contacts,
                            filteredContacts = filtered,
                            messageSummaries = summaries,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun buildActivitySummaries(messages: List<Message>): Map<Long, ContactActivity> {
        return messages.groupBy { it.contactId }.mapValues { (_, msgs) ->
            val latest = msgs.maxByOrNull { it.createdAt }
            ContactActivity(
                contactId = latest?.contactId ?: 0L,
                lastBody = latest?.body ?: "",
                lastAt = latest?.createdAt ?: 0L,
                lastDirection = latest?.direction ?: "",
                unreadCount = msgs.count { it.direction == "RECEIVED" && !it.isRead }
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            val filtered = applyFilter(
                it.allContacts, query, it.selectedFilter, it.sortBy, it.messageSummaries
            )
            it.copy(searchQuery = query, filteredContacts = filtered)
        }
    }

    fun onFilterChanged(filter: ContactFilter) {
        _uiState.update {
            val filtered = applyFilter(
                it.allContacts, it.searchQuery, filter, it.sortBy, it.messageSummaries
            )
            it.copy(selectedFilter = filter, filteredContacts = filtered)
        }
    }

    fun onSortChanged(sort: ContactSort) {
        appPreferences.contactSort = sort.name
        _uiState.update {
            val filtered = applyFilter(
                it.allContacts, it.searchQuery, it.selectedFilter, sort, it.messageSummaries
            )
            it.copy(sortBy = sort, filteredContacts = filtered)
        }
    }

    /**
     * Single-click save unsaved contact directly to device Phone Contacts book
     */
    fun onSaveToDevicePhone(contact: Contact) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingAction = true,
                    processingTitle = "Saving to Device Contacts..."
                )
            }
            try {
                val success = phoneContactWriter.saveContact(
                    name = contact.name,
                    phoneNumber = contact.phoneNumber,
                    email = contact.email
                )
                if (success) {
                    val updated = contact.copy(savedToPhone = true)
                    updateContactUseCase(updated)
                    _uiState.update {
                        it.copy(
                            toastSuccess = "'${contact.name}' saved to phone contacts!",
                            infoMessage = "Saved '${contact.name}' to device contact book"
                        )
                    }
                } else {
                    errorLogger.log(
                        action = "save_contact_to_phone",
                        message = "Failed to write '${contact.name}' to device contacts",
                        detail = "ContactsProvider write returned false"
                    )
                    _uiState.update {
                        it.copy(
                            toastError = "Failed to write contact to phone book",
                            error = "Could not write to ContactsProvider. Please check permissions."
                        )
                    }
                }
            } catch (e: Exception) {
                errorLogger.log(
                    action = "save_contact_to_phone",
                    message = "Error saving '${contact.name}' to phone book",
                    detail = e.message
                )
                _uiState.update {
                    it.copy(
                        toastError = "Error: ${e.message}",
                        error = "Phone save error: ${e.message}"
                    )
                }
            } finally {
                _uiState.update { it.copy(isProcessingAction = false) }
            }
        }
    }

    /**
     * Open device phone book picker and show contacts available to import
     */
    fun onOpenImportFromPhone() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingAction = true, processingTitle = "Loading Phone Contacts...") }
            try {
                val deviceContacts = deviceContactReader.readDeviceContacts()
                _uiState.update {
                    it.copy(
                        deviceContacts = deviceContacts,
                        showImportDialog = true
                    )
                }
            } catch (e: Exception) {
                errorLogger.log(
                    action = "load_phone_contacts",
                    message = "Could not read device phone contacts",
                    detail = e.message
                )
                _uiState.update { it.copy(toastError = "Could not read phone contacts: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isProcessingAction = false) }
            }
        }
    }

    fun onDismissImportDialog() {
        _uiState.update { it.copy(showImportDialog = false) }
    }

    /**
     * Import chosen contact from device phone book into Census registry
     */
    fun onImportContactSelected(deviceContact: DeviceContact) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showImportDialog = false,
                    isProcessingAction = true,
                    processingTitle = "Importing ${deviceContact.name}..."
                )
            }
            try {
                val newContact = Contact(
                    name = deviceContact.name,
                    phoneNumber = deviceContact.phoneNumber,
                    email = deviceContact.email,
                    savedToPhone = true,
                    channelPreference = "SMS"
                )
                addContactUseCase(newContact)
                _uiState.update {
                    it.copy(
                        toastSuccess = "Imported '${deviceContact.name}' from phone!",
                        infoMessage = "Contact imported into registry"
                    )
                }
            } catch (e: Exception) {
                errorLogger.log(
                    action = "import_phone_contact",
                    message = "Import of '${deviceContact.name}' failed",
                    detail = e.message
                )
                _uiState.update { it.copy(toastError = "Import failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isProcessingAction = false) }
            }
        }
    }

    fun onDeleteContact(contact: Contact) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingAction = true, processingTitle = "Deleting Contact...") }
            try {
                if (contact.id > 0) {
                    deleteContactUseCase(contact.id)
                    _uiState.update {
                        it.copy(
                            toastSuccess = "Deleted '${contact.name}'",
                            infoMessage = "Contact '${contact.name}' deleted successfully"
                        )
                    }
                }
            } catch (e: Exception) {
                errorLogger.log(
                    action = "delete_contact",
                    message = "Failed to delete '${contact.name}'",
                    detail = e.message
                )
                _uiState.update {
                    it.copy(
                        toastError = "Failed: ${e.message}",
                        error = "Delete failed: ${e.message}"
                    )
                }
            } finally {
                _uiState.update { it.copy(isProcessingAction = false) }
            }
        }
    }

    fun exportBackup(destination: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingAction = true, processingTitle = "Exporting Backup...") }
            try {
                jsonBackupService.exportToUri(destination)
                _uiState.update {
                    it.copy(
                        toastSuccess = "Backup exported!",
                        infoMessage = "Backup file saved successfully"
                    )
                }
            } catch (e: Exception) {
                errorLogger.log(
                    action = "export_backup",
                    message = "Backup export failed",
                    detail = e.message
                )
                _uiState.update {
                    it.copy(
                        toastError = "Export failed",
                        error = "Export failed: ${e.message}"
                    )
                }
            } finally {
                _uiState.update { it.copy(isProcessingAction = false) }
            }
        }
    }

    fun importBackup(source: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingAction = true, processingTitle = "Importing Registry...") }
            try {
                val summary = jsonBackupService.importFromUri(source)
                _uiState.update {
                    it.copy(
                        toastSuccess = "Registry imported!",
                        infoMessage = "Import complete: ${summary.contactsAdded} contacts added, " +
                                "${summary.contactsUpdated} updated, ${summary.messagesImported} messages, " +
                                "${summary.presetsImported} presets" +
                                if (summary.messagesSkipped > 0) " (${summary.messagesSkipped} duplicates skipped)" else ""
                    )
                }
            } catch (e: Exception) {
                errorLogger.log(
                    action = "import_backup",
                    message = "Backup import failed",
                    detail = e.message
                )
                _uiState.update {
                    it.copy(
                        toastError = "Import failed",
                        error = "Import failed: ${e.message}"
                    )
                }
            } finally {
                _uiState.update { it.copy(isProcessingAction = false) }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastSuccess = null, toastError = null) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun applyFilter(
        contacts: List<Contact>,
        query: String,
        filter: ContactFilter,
        sort: ContactSort = ContactSort.NAME,
        summaries: Map<Long, ContactActivity> = emptyMap()
    ): List<Contact> {
        val q = query.trim().lowercase()
        val filtered = contacts.filter { c ->
            val matchesQuery = q.isEmpty() ||
                    c.name.lowercase().contains(q) ||
                    c.phoneNumber.contains(q) ||
                    (c.houseNo?.lowercase()?.contains(q) == true) ||
                    (c.location?.lowercase()?.contains(q) == true) ||
                    (c.email?.lowercase()?.contains(q) == true)

            val matchesFilter = when (filter) {
                ContactFilter.ALL -> true
                ContactFilter.SAVED_TO_PHONE -> c.savedToPhone
                ContactFilter.NOT_SAVED -> !c.savedToPhone
            }

            matchesQuery && matchesFilter
        }

        return when (sort) {
            ContactSort.NAME -> filtered.sortedBy { it.name.lowercase() }
            ContactSort.LAST_ACTIVITY -> filtered.sortedWith(
                compareByDescending<Contact> { summaries[it.id]?.lastAt ?: 0L }
                    .thenBy { it.name.lowercase() }
            )
        }
    }
}
