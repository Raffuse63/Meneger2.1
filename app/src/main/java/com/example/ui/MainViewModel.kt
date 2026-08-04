package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
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

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val noteDao = db.noteDao()
    private val dueDao = db.peopleDueDao()
    private val trackerDao = db.trackerDao()
    private val bazarDao = db.bazarDao()
    private val budgetDao = db.budgetDao()

    val notes: StateFlow<List<NoteEntity>> = noteDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dues: StateFlow<List<PeopleDueEntity>> = dueDao.getAllDues()
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
    fun addNote(title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = "03 Aug 2026, 12:00 PM"
            noteDao.insertNote(NoteEntity(title = title, content = content, dateString = dateStr))
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.updateNote(note)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.deleteNote(note)
        }
    }

    // --- ACCOUNT / DUE OPERATIONS ---
    fun addDue(name: String, amountOwed: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val initial = if (name.isNotBlank()) name.first().toString() else "র"
            dueDao.insertDue(PeopleDueEntity(name = name, initial = initial, amountOwed = amountOwed))
        }
    }

    fun updateDue(due: PeopleDueEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dueDao.updateDue(due)
        }
    }

    fun deleteDue(due: PeopleDueEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dueDao.deleteDue(due)
        }
    }

    // --- TRACKER OPERATIONS ---
    fun addTrackerRecord(title: String, amount: Double, isExpense: Boolean, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val record = TrackerRecordEntity(
                title = title,
                amount = amount,
                isExpense = isExpense,
                dateString = "03 Aug, 12:00 PM",
                category = category
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
}
