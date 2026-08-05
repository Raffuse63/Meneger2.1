package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.GoogleAuthHelper
import com.example.data.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppTab {
    NOTEBOOK,
    ACCOUNT,
    TRACKER,
    BAZAR,
    BUDGET
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val googleAuthHelper = GoogleAuthHelper()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(googleAuthHelper.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val noteDao = db.noteDao()
    private val dueDao = db.peopleDueDao()
    private val personTransactionDao = db.personTransactionDao()
    private val trackerDao = db.trackerDao()
    private val bazarDao = db.bazarDao()
    private val budgetDao = db.budgetDao()

    val notes: StateFlow<List<NoteEntity>> = noteDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dues: StateFlow<List<PeopleDueEntity>> = dueDao.getAllDues()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<PersonTransactionEntity>> = personTransactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<TransactionPaymentEntity>> = personTransactionDao.getAllPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trackerRecords: StateFlow<List<TrackerRecordEntity>> = trackerDao.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bazarItems: StateFlow<List<BazarItemEntity>> = bazarDao.getAllBazarItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetExpenses: StateFlow<List<BudgetExpenseEntity>> = budgetDao.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Tab
    private val _currentTab = MutableStateFlow(AppTab.NOTEBOOK)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // --- NOTE OPERATIONS ---
    fun addNote(title: String, content: String, checklistItems: List<NoteChecklistItem> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = try {
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                sdf.format(java.util.Date())
            } catch (e: Exception) {
                "04 Aug 2026, 12:00 PM"
            }
            val checklistJson = NoteChecklistSerializer.serialize(checklistItems)
            noteDao.insertNote(NoteEntity(title = title, content = content, dateString = dateStr, checklistJson = checklistJson))
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.updateNote(note)
        }
    }

    fun toggleNoteChecklistItem(note: NoteEntity, index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = note.getChecklistItems().toMutableList()
            if (index in items.indices) {
                val current = items[index]
                items[index] = current.copy(isChecked = !current.isChecked)
                val updatedNote = note.copy(checklistJson = NoteChecklistSerializer.serialize(items))
                noteDao.updateNote(updatedNote)
            }
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.deleteNote(note)
        }
    }

    // --- ACCOUNT / DUE OPERATIONS ---
    fun addDue(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val initial = if (name.isNotBlank()) name.first().toString() else "র"
            dueDao.insertDue(PeopleDueEntity(name = name.trim(), initial = initial, amountOwed = 0.0, amountReceivable = 0.0))
        }
    }

    fun updateDueName(due: PeopleDueEntity, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = newName.trim()
            val initial = if (trimmed.isNotBlank()) trimmed.first().toString() else due.initial
            dueDao.updateDue(due.copy(name = trimmed, initial = initial))
        }
    }

    fun deleteDue(due: PeopleDueEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dueDao.deleteDue(due)
        }
    }

    fun addPersonTransaction(personId: Long, details: String, amount: Double, isGive: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateTimeStr = getCurrentFormattedDateTime()
            personTransactionDao.insertTransaction(
                PersonTransactionEntity(
                    personId = personId,
                    details = details,
                    amount = amount,
                    dateTime = dateTimeStr,
                    isGive = isGive
                )
            )
            recalculatePersonDue(personId)
        }
    }

    fun addTransactionPayment(transactionId: Long, personId: Long, paidAmount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateTimeStr = getCurrentFormattedDateTime()
            personTransactionDao.insertPayment(
                TransactionPaymentEntity(
                    transactionId = transactionId,
                    paidAmount = paidAmount,
                    dateTime = dateTimeStr
                )
            )
            recalculatePersonDue(personId)
        }
    }

    private suspend fun recalculatePersonDue(personId: Long) {
        val personDuesList = dueDao.getAllDues().firstOrNull() ?: return
        val duePerson = personDuesList.find { it.id == personId } ?: return
        val allTx = personTransactionDao.getAllTransactions().firstOrNull() ?: emptyList()
        val allPm = personTransactionDao.getAllPayments().firstOrNull() ?: emptyList()

        val personTx = allTx.filter { it.personId == personId }
        val personTxIds = personTx.map { it.id }.toSet()
        val personPm = allPm.filter { it.transactionId in personTxIds }

        var totalReceivable = 0.0 // I give (পাওনা +)
        var totalOwed = 0.0       // I receive (দেনা -)

        personTx.forEach { tx ->
            val txPaymentsSum = personPm.filter { it.transactionId == tx.id }.sumOf { it.paidAmount }
            val remaining = (tx.amount - txPaymentsSum).coerceAtLeast(0.0)
            if (tx.isGive) {
                totalReceivable += remaining
            } else {
                totalOwed += remaining
            }
        }

        dueDao.updateDue(duePerson.copy(amountReceivable = totalReceivable, amountOwed = totalOwed))
    }

    private fun getCurrentFormattedDateTime(): String {
        return try {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
            sdf.format(java.util.Date())
        } catch (e: Exception) {
            "04 Aug 2026, 12:00 PM"
        }
    }

    // --- TRACKER OPERATIONS ---
    fun addTrackerRecord(
        title: String,
        amount: Double,
        isExpense: Boolean,
        category: String,
        description: String = "",
        year: Int = 2026,
        month: String = "August",
        day: String = "1",
        dateString: String = "04 Aug 2026"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val record = TrackerRecordEntity(
                title = title,
                amount = amount,
                isExpense = isExpense,
                dateString = dateString,
                category = category,
                description = description,
                year = year,
                month = month,
                day = day
            )
            trackerDao.insertRecord(record)
        }
    }

    fun updateTrackerRecord(record: TrackerRecordEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            trackerDao.updateRecord(record)
        }
    }

    fun deleteTrackerRecord(record: TrackerRecordEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            trackerDao.deleteRecord(record)
        }
    }

    // --- BAZAR OPERATIONS ---
    fun addBazarItem(title: String, targetPrice: Double, unitQuantity: String) {
        viewModelScope.launch(Dispatchers.IO) {
            bazarDao.insertBazarItem(BazarItemEntity(title = title, targetPrice = targetPrice, unitQuantity = unitQuantity))
        }
    }

    fun updateBazarSpent(item: BazarItemEntity, newSpent: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            bazarDao.updateBazarItem(item.copy(actualSpent = newSpent))
        }
    }

    fun deleteBazarItem(item: BazarItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            bazarDao.deleteBazarItem(item)
        }
    }

    // --- BUDGET OPERATIONS ---
    fun addBudgetExpense(title: String, tag: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetDao.insertExpense(BudgetExpenseEntity(title = title, tag = tag, dateText = "আজ", amount = amount))
        }
    }

    fun deleteBudgetExpense(expense: BudgetExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetDao.deleteExpense(expense)
        }
    }

    // --- AUTH OPERATIONS ---
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _isSigningIn.value = true
            _authError.value = null
            val result = googleAuthHelper.signInWithGoogle(context)
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
            } else {
                _authError.value = result.exceptionOrNull()?.localizedMessage ?: "গুগল সাইন ইন সফল হয়নি"
            }
            _isSigningIn.value = false
        }
    }

    fun signOut() {
        googleAuthHelper.signOut()
        _currentUser.value = null
    }

    fun clearAuthError() {
        _authError.value = null
    }
}
