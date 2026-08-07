package com.example

import com.example.data.AppDatabase
import com.example.data.MarketItemDao
import com.example.data.MarketItem
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.GoogleAuthProvider
import coil.compose.rememberAsyncImagePainter
import android.content.pm.PackageManager
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================
// ROOM DATABASE & ENTITIES
// ============================================================================

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val openingBalance: Double = 0.0,
    val monthlySalary: Double = 0.0,
    val isSalaryIncluded: Boolean = false
)

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val dateTime: Long, // Timestamp
    val note: String,
    val personName: String = "General",
    val paidAmount: Double = 0.0,
    val repaymentsCsv: String = "",
    val isPersonal: Boolean = false
)

@Entity(tableName = "notices")
data class NoticeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val timestamp: Long,
    val colorHex: String // Pastel color hex, e.g., "#FFF9C4"
)

@Dao
interface FinanceDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun getProfileFlow(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun getProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM transactions ORDER BY dateTime DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM notices ORDER BY timestamp DESC")
    fun getAllNoticesFlow(): Flow<List<NoticeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: NoticeEntity)

    @Delete
    suspend fun deleteNotice(notice: NoticeEntity)

    @Query("SELECT * FROM persons ORDER BY id ASC")
    fun getAllPersonsFlow(): Flow<List<PersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM persons")
    suspend fun clearPersons()

    @Query("DELETE FROM notices")
    suspend fun clearNotices()

    @Query("DELETE FROM profile")
    suspend fun clearProfile()
}

@Database(
    entities = [ProfileEntity::class, TransactionEntity::class, NoticeEntity::class, PersonEntity::class],
    version = 5,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        private val INSTANCES = HashMap<String, FinanceDatabase>()

        fun getDatabase(context: Context, accountName: String = "Default"): FinanceDatabase {
            val dbName = if (accountName == "Default") "finance_database" else "finance_database_$accountName"
            return synchronized(this) {
                INSTANCES.getOrPut(dbName) {
                    Room.databaseBuilder(
                        context.applicationContext,
                        FinanceDatabase::class.java,
                        dbName
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                }
            }
        }
    }
}

// ============================================================================
// VIEWMODEL FOR STATE MANAGEMENT
// ============================================================================

class FinanceViewModel(
    val dao: FinanceDao,
    val marketItemDao: MarketItemDao,
    val categoryDao: com.example.data.local.dao.CategoryDao? = null,
    val expenseDao: com.example.data.local.dao.ExpenseDao? = null
) : ViewModel() {
    val profile = dao.getProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val transactions = dao.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val notices = dao.getAllNoticesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val persons = dao.getAllPersonsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val marketItems = marketItemDao.getAllItems()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories = categoryDao?.getAllCategories()
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        ?: MutableStateFlow(emptyList())

    val expenses = expenseDao?.getAllExpenses()
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        ?: MutableStateFlow(emptyList())

    // Firebase and Google Sign-In state
    var currentUserState by mutableStateOf<FirebaseUser?>(
        try { FirebaseAuth.getInstance().currentUser } catch (e: Throwable) { null }
    )
    var isSyncingFromCloud by mutableStateOf(false)
    var isConnectingToCloud by mutableStateOf(false)
    val isLocalDataLoadedState = MutableStateFlow(false)

    init {
        // Wait for first emission from database before enabling auto-upload to cloud, and auto-restore if empty
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbTxsDef = async { dao.getAllTransactionsFlow().first() }
                val dbPersonsDef = async { dao.getAllPersonsFlow().first() }
                val dbNoticesDef = async { dao.getAllNoticesFlow().first() }
                val dbMarketDef = async { marketItemDao.getAllItems().first() }
                
                val dbTxs = dbTxsDef.await()
                val dbPersons = dbPersonsDef.await()
                val dbNotices = dbNoticesDef.await()
                val dbMarket = dbMarketDef.await()
                dao.getProfileFlow().first()
                
                val user = try { FirebaseAuth.getInstance().currentUser } catch (e: Throwable) { null }
                if (user != null) {
                    if (dbTxs.isEmpty() && dbPersons.isEmpty() && dbNotices.isEmpty() && dbMarket.isEmpty()) {
                        syncFromCloud(user.uid)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLocalDataLoadedState.value = true
            }
        }

        // Observe local database changes and upload to cloud if signed in
        viewModelScope.launch(Dispatchers.IO) {
            val dbChanges = combine(listOf(transactions, persons, notices, profile, marketItems, categories, expenses)) {
                true
            }
            combine(dbChanges, isLocalDataLoadedState) { _, loaded ->
                loaded
            }.collect { loaded ->
                try {
                    val user = try { FirebaseAuth.getInstance().currentUser } catch (e: Throwable) { null }
                    if (user != null && !isSyncingFromCloud && loaded) {
                        val json = generateBackupJsonString()
                        if (json.isNotEmpty()) {
                            val dbRef = FirebaseDatabase.getInstance("https://overtime-9a9a5-default-rtdb.asia-southeast1.firebasedatabase.app")
                                .getReference("users")
                                .child(user.uid)
                                .child("data_v1")
                            dbRef.setValue(json)
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        // Listen for Firebase Auth changes
        try {
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                try {
                    val user = auth.currentUser
                    val oldUser = currentUserState
                    currentUserState = user
                    if (user != null && oldUser?.uid != user.uid) {
                        // User has signed in or switched, download from cloud
                        syncFromCloud(user.uid)
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun syncFromCloud(uid: String) {
        try {
            isConnectingToCloud = true
            val dbRef = FirebaseDatabase.getInstance("https://overtime-9a9a5-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users")
                .child(uid)
                .child("data_v1")
            
            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val jsonText = snapshot.getValue(String::class.java)
                    if (jsonText != null && jsonText.isNotEmpty()) {
                        viewModelScope.launch {
                            isSyncingFromCloud = true
                            try {
                                restoreFromJsonString(jsonText)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isSyncingFromCloud = false
                                isConnectingToCloud = false
                            }
                        }
                    } else {
                        // No data on cloud, save local data if not empty
                        isConnectingToCloud = false
                        val localJson = generateBackupJsonString()
                        if (localJson.isNotEmpty() && (transactions.value.isNotEmpty() || persons.value.isNotEmpty() || notices.value.isNotEmpty() || marketItems.value.isNotEmpty())) {
                            dbRef.setValue(localJson)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    isConnectingToCloud = false
                }
            })
        } catch (e: Throwable) {
            e.printStackTrace()
            isConnectingToCloud = false
        }
    }

    fun logoutAndClearLocal(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            isSyncingFromCloud = true
            try {
                dao.clearTransactions()
                dao.clearPersons()
                dao.clearNotices()
                dao.clearProfile()
                marketItemDao.clearAllItems()
                // Default profile setup
                dao.insertProfile(ProfileEntity(1, 0.0, 0.0, false))
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSyncingFromCloud = false
                try {
                    FirebaseAuth.getInstance().signOut()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                currentUserState = null
                onComplete()
            }
        }
    }

    // Active Navigation Tab: "ACCOUNT", "TRACKER", "NOTICE"
    var currentTab by mutableStateOf("TRACKER")

    // Account inputs (Legacy, kept to avoid compile errors, but visual account screen is removed)
    var openingBalanceInput by mutableStateOf("")
    var monthlySalaryInput by mutableStateOf("")

    // Person inputs
    var selectedPersonName by mutableStateOf("")
    var newPersonNameInput by mutableStateOf("")
    var filterPersonName by mutableStateOf("ALL")

    // Tracker inputs
    var activeFormType by mutableStateOf("EXPENSE") // "ALL", "EXPENSE" or "INCOME"
    var amountInput by mutableStateOf("")
    var categoryInput by mutableStateOf("")
    var dateTimeInput by mutableStateOf("")
    var noteInput by mutableStateOf("")

    // Edit Transaction mode
    var editingTransactionId by mutableStateOf<Int?>(null)

    // Filter values
    var filterType by mutableStateOf("ALL")   // "ALL", "INCOME", "EXPENSE"
    var filterMonth by mutableStateOf(Calendar.getInstance().get(Calendar.MONTH).toString())  // Default current month
    var filterYear by mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString())   // Default current year
    var filterDay by mutableStateOf("ALL")    // "ALL", "1" - "31"
    var filterCategoryQuery by mutableStateOf("")

    // List toggle: Default last 5 records
    var showAllTransactions by mutableStateOf(false)
    var listGroupingMode by mutableStateOf("NONE") // "NONE", "CATEGORY", "GROUP"

    // Notice Inputs
    var noticeTitleInput by mutableStateOf("")
    var noticeContentInput by mutableStateOf("")
    var selectedNoticeColorHex by mutableStateOf("#FFF9C4") // Default yellow
    var showAddNoticeDialog by mutableStateOf(false)
    var showAddPersonDialog by mutableStateOf(false)
    var showAddTransactionDialog by mutableStateOf(false)
    var editingNotice by mutableStateOf<NoticeEntity?>(null)

    // Dialog state for Double Tap note review
    var selectedTransactionForDetails by mutableStateOf<TransactionEntity?>(null)

    // Backup and Restore States
    var showBackupDialog by mutableStateOf(false)
    var showRestoreDialog by mutableStateOf(false)
    var backupJsonText by mutableStateOf("")
    var restoreInputText by mutableStateOf("")

    fun generateBackupJsonString(): String {
        return try {
            val rootObj = JSONObject()
            rootObj.put("version", 1)

            // Profile
            val p = profile.value
            if (p != null) {
                val pObj = JSONObject()
                pObj.put("id", p.id)
                pObj.put("openingBalance", p.openingBalance)
                pObj.put("monthlySalary", p.monthlySalary)
                pObj.put("isSalaryIncluded", p.isSalaryIncluded)
                rootObj.put("profile", pObj)
            }

            // Persons
            val personsArray = JSONArray()
            persons.value.forEach { person ->
                val personObj = JSONObject()
                personObj.put("id", person.id)
                personObj.put("name", person.name)
                personsArray.put(personObj)
            }
            rootObj.put("persons", personsArray)

            // Transactions
            val txArray = JSONArray()
            transactions.value.forEach { tx ->
                val txObj = JSONObject()
                txObj.put("id", tx.id)
                txObj.put("amount", tx.amount)
                txObj.put("type", tx.type)
                txObj.put("category", tx.category)
                txObj.put("dateTime", tx.dateTime)
                txObj.put("note", tx.note)
                txObj.put("personName", tx.personName)
                txObj.put("paidAmount", tx.paidAmount)
                txObj.put("repaymentsCsv", tx.repaymentsCsv)
                txObj.put("isPersonal", tx.isPersonal)
                txArray.put(txObj)
            }
            rootObj.put("transactions", txArray)

            // Notices
            val noticesArray = JSONArray()
            notices.value.forEach { notice ->
                val noticeObj = JSONObject()
                noticeObj.put("id", notice.id)
                noticeObj.put("content", notice.content)
                noticeObj.put("timestamp", notice.timestamp)
                noticeObj.put("colorHex", notice.colorHex)
                noticesArray.put(noticeObj)
            }
            rootObj.put("notices", noticesArray)

            // Market Items (Bazaar)
            val marketItemsArray = JSONArray()
            marketItems.value.forEach { item ->
                val itemObj = JSONObject()
                itemObj.put("id", item.id)
                itemObj.put("description", item.description)
                itemObj.put("quantity", item.quantity)
                itemObj.put("targetPrice", item.targetPrice)
                itemObj.put("actualPrice", item.actualPrice)
                itemObj.put("isActive", item.isActive)
                itemObj.put("timestamp", item.timestamp)
                marketItemsArray.put(itemObj)
            }
            rootObj.put("market_items", marketItemsArray)

            // Categories
            val categoriesArray = JSONArray()
            categories.value.forEach { cat ->
                val catObj = JSONObject()
                catObj.put("id", cat.id)
                catObj.put("name", cat.name)
                catObj.put("budget", cat.budget)
                categoriesArray.put(catObj)
            }
            rootObj.put("categories", categoriesArray)

            // Expenses
            val expensesArray = JSONArray()
            expenses.value.forEach { exp ->
                val expObj = JSONObject()
                expObj.put("id", exp.id)
                expObj.put("description", exp.description)
                expObj.put("amount", exp.amount)
                expObj.put("date", exp.date)
                expObj.put("categoryId", exp.categoryId)
                expensesArray.put(expObj)
            }
            rootObj.put("expenses", expensesArray)

            rootObj.toString(2)
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun restoreFromJsonString(jsonString: String): Boolean {
        return try {
            val rootObj = JSONObject(jsonString)
            if (!rootObj.has("transactions") && !rootObj.has("persons") && !rootObj.has("notices") && !rootObj.has("profile") && !rootObj.has("market_items") && !rootObj.has("categories") && !rootObj.has("expenses")) {
                return false
            }

            // Clear tables
            dao.clearTransactions()
            dao.clearPersons()
            dao.clearNotices()
            dao.clearProfile()
            marketItemDao.clearAllItems()
            categoryDao?.deleteAllCategories()
            expenseDao?.deleteAllExpenses()

            // Restore Profile
            if (rootObj.has("profile")) {
                val pObj = rootObj.getJSONObject("profile")
                val profileEntity = ProfileEntity(
                    id = pObj.optInt("id", 1),
                    openingBalance = pObj.optDouble("openingBalance", 0.0),
                    monthlySalary = pObj.optDouble("monthlySalary", 0.0),
                    isSalaryIncluded = pObj.optBoolean("isSalaryIncluded", false)
                )
                dao.insertProfile(profileEntity)
            } else {
                dao.insertProfile(ProfileEntity(1, 0.0, 0.0, false))
            }

            // Restore Persons
            if (rootObj.has("persons")) {
                val pArray = rootObj.getJSONArray("persons")
                for (i in 0 until pArray.length()) {
                    val pObj = pArray.getJSONObject(i)
                    val person = PersonEntity(
                        id = pObj.getInt("id"),
                        name = pObj.getString("name")
                    )
                    dao.insertPerson(person)
                }
            }

            // Restore Transactions
            if (rootObj.has("transactions")) {
                val tArray = rootObj.getJSONArray("transactions")
                for (i in 0 until tArray.length()) {
                    val tObj = tArray.getJSONObject(i)
                    val tx = TransactionEntity(
                        id = tObj.getInt("id"),
                        amount = tObj.getDouble("amount"),
                        type = tObj.getString("type"),
                        category = tObj.getString("category"),
                        dateTime = tObj.getLong("dateTime"),
                        note = tObj.optString("note", ""),
                        personName = tObj.optString("personName", "General"),
                        paidAmount = tObj.optDouble("paidAmount", 0.0),
                        repaymentsCsv = tObj.optString("repaymentsCsv", ""),
                        isPersonal = tObj.optBoolean("isPersonal", false)
                    )
                    dao.insertTransaction(tx)
                }
            }

            // Restore Notices
            if (rootObj.has("notices")) {
                val nArray = rootObj.getJSONArray("notices")
                for (i in 0 until nArray.length()) {
                    val nObj = nArray.getJSONObject(i)
                    val notice = NoticeEntity(
                        id = nObj.getInt("id"),
                        content = nObj.getString("content"),
                        timestamp = nObj.getLong("timestamp"),
                        colorHex = nObj.optString("colorHex", "#FFF9C4")
                    )
                    dao.insertNotice(notice)
                }
            }

            // Restore Market Items
            if (rootObj.has("market_items")) {
                val mArray = rootObj.getJSONArray("market_items")
                for (i in 0 until mArray.length()) {
                    val mObj = mArray.getJSONObject(i)
                    val item = MarketItem(
                        id = mObj.getInt("id"),
                        description = mObj.getString("description"),
                        quantity = mObj.optString("quantity", ""),
                        targetPrice = mObj.optDouble("targetPrice", 0.0),
                        actualPrice = mObj.optDouble("actualPrice", 0.0),
                        isActive = mObj.optBoolean("isActive", true),
                        timestamp = mObj.optLong("timestamp", System.currentTimeMillis())
                    )
                    marketItemDao.insertItem(item)
                }
            }

            // Restore Categories
            if (rootObj.has("categories") && categoryDao != null) {
                val catArray = rootObj.getJSONArray("categories")
                val catList = mutableListOf<com.example.data.local.entity.CategoryEntity>()
                for (i in 0 until catArray.length()) {
                    val cObj = catArray.getJSONObject(i)
                    catList.add(
                        com.example.data.local.entity.CategoryEntity(
                            id = cObj.optLong("id", 0L),
                            name = cObj.getString("name"),
                            budget = cObj.optDouble("budget", 0.0)
                        )
                    )
                }
                categoryDao.insertCategories(catList)
            }

            // Restore Expenses
            if (rootObj.has("expenses") && expenseDao != null) {
                val expArray = rootObj.getJSONArray("expenses")
                val expList = mutableListOf<com.example.data.local.entity.ExpenseEntity>()
                for (i in 0 until expArray.length()) {
                    val eObj = expArray.getJSONObject(i)
                    expList.add(
                        com.example.data.local.entity.ExpenseEntity(
                            id = eObj.optLong("id", 0L),
                            description = eObj.getString("description"),
                            amount = eObj.getDouble("amount"),
                            date = eObj.getLong("date"),
                            categoryId = eObj.getLong("categoryId")
                        )
                    )
                }
                expenseDao.insertExpenses(expList)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Dialog confirmation states
    var showDeleteConfirmDialog by mutableStateOf(false)
    var pendingDeleteAction by mutableStateOf<(() -> Unit)?>(null)
    var deleteConfirmTitle by mutableStateOf("")
    var deleteConfirmMessage by mutableStateOf("")

    fun confirmDelete(title: String, message: String, action: () -> Unit) {
        deleteConfirmTitle = title
        deleteConfirmMessage = message
        pendingDeleteAction = action
        showDeleteConfirmDialog = true
    }

    // Undo states
    var lastDeletedPerson by mutableStateOf<PersonEntity?>(null)
    var lastDeletedPersonTransactions by mutableStateOf<List<TransactionEntity>>(emptyList())
    var lastDeletedTransaction by mutableStateOf<TransactionEntity?>(null)
    var lastDeletedNotice by mutableStateOf<NoticeEntity?>(null)

    fun deletePersonWithUndo(person: PersonEntity, onShowSnackbar: (String, () -> Unit) -> Unit) {
        viewModelScope.launch {
            lastDeletedPerson = person
            val allTxs = dao.getAllTransactionsFlow().first()
            lastDeletedPersonTransactions = allTxs.filter { it.personName.trim().equals(person.name.trim(), ignoreCase = true) }
            
            dao.deletePerson(person)
            lastDeletedPersonTransactions.forEach { dao.deleteTransaction(it) }

            onShowSnackbar("${person.name} has been deleted!") {
                viewModelScope.launch {
                    val p = lastDeletedPerson
                    if (p != null) {
                        dao.insertPerson(p)
                        lastDeletedPersonTransactions.forEach { dao.insertTransaction(it) }
                        lastDeletedPerson = null
                        lastDeletedPersonTransactions = emptyList()
                    }
                }
            }
        }
    }

    fun deleteTransactionWithUndo(tx: TransactionEntity, onShowSnackbar: (String, () -> Unit) -> Unit) {
        viewModelScope.launch {
            lastDeletedTransaction = tx
            dao.deleteTransaction(tx)
            
            onShowSnackbar("Transaction has been deleted!") {
                viewModelScope.launch {
                    val t = lastDeletedTransaction
                    if (t != null) {
                        dao.insertTransaction(t)
                        lastDeletedTransaction = null
                    }
                }
            }
        }
    }

    fun deleteNoticeWithUndo(notice: NoticeEntity, onShowSnackbar: (String, () -> Unit) -> Unit) {
        viewModelScope.launch {
            lastDeletedNotice = notice
            dao.deleteNotice(notice)
            
            onShowSnackbar("Note has been deleted!") {
                viewModelScope.launch {
                    val n = lastDeletedNotice
                    if (n != null) {
                        dao.insertNotice(n)
                        lastDeletedNotice = null
                    }
                }
            }
        }
    }

    fun updatePersonName(person: PersonEntity, newName: String) {
        if (newName.trim().isEmpty()) return
        viewModelScope.launch {
            val updatedPerson = person.copy(name = newName.trim())
            dao.insertPerson(updatedPerson)
            
            val txs = dao.getAllTransactionsFlow().first()
            txs.forEach { tx ->
                if (tx.personName.trim().equals(person.name.trim(), ignoreCase = true)) {
                    dao.updateTransaction(tx.copy(personName = newName.trim()))
                }
            }
        }
    }

    init {
        resetTrackerFormDateTime()
        viewModelScope.launch {
            val existing = dao.getProfile()
            if (existing == null) {
                dao.insertProfile(ProfileEntity(1, 0.0, 0.0, false))
            } else {
                openingBalanceInput = if (existing.openingBalance == 0.0) "" else existing.openingBalance.toString()
                monthlySalaryInput = if (existing.monthlySalary == 0.0) "" else existing.monthlySalary.toString()
            }
            try {
                val list = dao.getAllPersonsFlow().first()
                // Do not insert "সাধারণ" default person anymore
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun resetTrackerFormDateTime() {
        dateTimeInput = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    }

    fun addPerson() {
        val name = newPersonNameInput.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            dao.insertPerson(PersonEntity(name = name))
            newPersonNameInput = ""
        }
    }

    fun deletePerson(person: PersonEntity) {
        viewModelScope.launch {
            dao.deletePerson(person)
        }
    }

    fun updateProfileSettings() {
        val op = openingBalanceInput.toDoubleOrNull() ?: 0.0
        val sal = monthlySalaryInput.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            val current = dao.getProfile() ?: ProfileEntity()
            dao.insertProfile(current.copy(openingBalance = op, monthlySalary = sal))
        }
    }

    fun toggleSalaryInclusion(included: Boolean) {
        viewModelScope.launch {
            val current = dao.getProfile() ?: ProfileEntity()
            dao.insertProfile(current.copy(isSalaryIncluded = included))
        }
    }

    fun saveTransaction() {
        val amount = amountInput.toDoubleOrNull() ?: 0.0
        if (amount <= 0.0) return

        val category = categoryInput.trim().ifEmpty { "Others 🪙" }
        val parsedTime = parseDateTime(dateTimeInput)
        val person = selectedPersonName.trim().ifEmpty { "General" }

        viewModelScope.launch {
            if (editingTransactionId != null) {
                dao.updateTransaction(
                    TransactionEntity(
                        id = editingTransactionId!!,
                        amount = amount,
                        type = activeFormType,
                        category = category,
                        dateTime = parsedTime,
                        note = noteInput.trim(),
                        personName = person,
                        isPersonal = false
                    )
                )
                editingTransactionId = null
            } else {
                dao.insertTransaction(
                    TransactionEntity(
                        amount = amount,
                        type = activeFormType,
                        category = category,
                        dateTime = parsedTime,
                        note = noteInput.trim(),
                        personName = person,
                        isPersonal = false
                    )
                )
            }
            // Clear inputs and reset dateTime
            amountInput = ""
            categoryInput = ""
            noteInput = ""
            resetTrackerFormDateTime()
        }
    }

    fun startEditingTransaction(tx: TransactionEntity) {
        editingTransactionId = tx.id
        activeFormType = tx.type
        amountInput = tx.amount.toString()
        categoryInput = tx.category
        dateTimeInput = formatDateTime(tx.dateTime)
        noteInput = tx.note
        selectedPersonName = tx.personName
        currentTab = "TRACKER" // Navigate to tracker to edit
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            dao.deleteTransaction(tx)
        }
    }

    fun cancelEditing() {
        editingTransactionId = null
        amountInput = ""
        categoryInput = ""
        noteInput = ""
        resetTrackerFormDateTime()
    }

    fun saveNotice() {
        val titleInput = noticeTitleInput.trim()
        val contentInput = noticeContentInput.trim()
        if (titleInput.isEmpty() && contentInput.isEmpty()) return

        val finalTitle: String
        val finalContent: String
        if (titleInput.isNotEmpty()) {
            finalTitle = titleInput
            finalContent = contentInput
        } else {
            val lines = contentInput.lines()
            if (lines.isNotEmpty()) {
                finalTitle = lines.first().trim()
                finalContent = lines.drop(1).joinToString("\n").trim()
            } else {
                finalTitle = ""
                finalContent = ""
            }
        }

        viewModelScope.launch {
            val combinedContent = "$finalTitle===NOTE_TITLE===$finalContent"
            val currentEditing = editingNotice
            if (currentEditing != null) {
                dao.insertNotice(
                    currentEditing.copy(
                        content = combinedContent,
                        timestamp = System.currentTimeMillis()
                    )
                )
                editingNotice = null
            } else {
                dao.insertNotice(
                    NoticeEntity(
                        content = combinedContent,
                        timestamp = System.currentTimeMillis(),
                        colorHex = selectedNoticeColorHex
                    )
                )
            }
            noticeContentInput = ""
            noticeTitleInput = ""
        }
    }

    fun updateNotice(notice: NoticeEntity) {
        viewModelScope.launch {
            dao.insertNotice(notice)
        }
    }

    fun moveNoticeUp(notice: NoticeEntity, currentList: List<NoticeEntity>) {
        val index = currentList.indexOfFirst { it.id == notice.id }
        if (index > 0) {
            val prevNotice = currentList[index - 1]
            val higherTimestamp = maxOf(notice.timestamp, prevNotice.timestamp) + 1000L
            val lowerTimestamp = minOf(notice.timestamp, prevNotice.timestamp) - 1000L
            viewModelScope.launch {
                dao.insertNotice(notice.copy(timestamp = higherTimestamp))
                dao.insertNotice(prevNotice.copy(timestamp = lowerTimestamp))
            }
        }
    }

    fun moveNoticeDown(notice: NoticeEntity, currentList: List<NoticeEntity>) {
        val index = currentList.indexOfFirst { it.id == notice.id }
        if (index >= 0 && index < currentList.size - 1) {
            val nextNotice = currentList[index + 1]
            val higherTimestamp = maxOf(notice.timestamp, nextNotice.timestamp) + 1000L
            val lowerTimestamp = minOf(notice.timestamp, nextNotice.timestamp) - 1000L
            viewModelScope.launch {
                dao.insertNotice(nextNotice.copy(timestamp = higherTimestamp))
                dao.insertNotice(notice.copy(timestamp = lowerTimestamp))
            }
        }
    }

    fun deleteNotice(notice: NoticeEntity) {
        viewModelScope.launch {
            dao.deleteNotice(notice)
        }
    }

    // Helper parser/formatters
    private fun parseDateTime(str: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.parse(str)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

class FinanceViewModelFactory(
    private val dao: FinanceDao,
    private val marketItemDao: MarketItemDao,
    private val categoryDao: com.example.data.local.dao.CategoryDao,
    private val expenseDao: com.example.data.local.dao.ExpenseDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(dao, marketItemDao, categoryDao, expenseDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

fun initFirebase(context: Context) {
    try {
        if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            val app = com.google.firebase.FirebaseApp.initializeApp(context.applicationContext)
            if (app == null) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("1:989102272624:android:0130f182c0a9fd4027c4b5")
                    .setApiKey("AIzaSyDcZaDSy9I9Y26Ga4n7ErllYgLp0Zq-6nk")
                    .setDatabaseUrl("https://overtime-9a9a5-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .setProjectId("overtime-9a9a5")
                    .setStorageBucket("overtime-9a9a5.firebasestorage.app")
                    .setGcmSenderId("989102272624")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(context.applicationContext, options)
            }
        }
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

// ============================================================================
// MAIN ACTIVITY & COMPOSABLE LAYOUT
// ============================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initFirebase(this)

        setContent {
            val context = LocalContext.current
            val db = remember { FinanceDatabase.getDatabase(context, "Default") }
            val marketDb = remember { AppDatabase.getDatabase(context) }
            val localDb = remember { com.example.data.local.AppDatabase.getDatabase(context) }
            val viewModelFactory = remember {
                FinanceViewModelFactory(
                    db.financeDao(),
                    marketDb.marketItemDao(),
                    localDb.categoryDao(),
                    localDb.expenseDao()
                )
            }

            MaterialTheme(
                colorScheme = androidx.compose.material3.lightColorScheme(
                    primary = Color(0xFF2E7D32), // Darker green for light theme readability
                    secondary = Color(0xFF1976D2), // Darker blue for readability
                    background = Color(0xFFFFFFFF), // White background
                    surface = Color(0xFFF5F6FA), // Off-white/Light grey card
                    onBackground = Color(0xFF1C1B1F), // Dark text on background
                    onSurface = Color(0xFF1C1B1F) // Dark text on surface
                )
            ) {
                val viewModel: FinanceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = viewModelFactory
                )
                FinanceApp(
                    viewModel = viewModel
                )
            }
        }
    }
}

// ============================================================================
// APP ENTRY VIEW
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinanceApp(
    viewModel: FinanceViewModel
) {
    val context = LocalContext.current
    val marketViewModel: com.example.ui.MarketViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = com.example.ui.MarketViewModel.Factory)
    val expenseViewModel: com.example.ui.viewmodel.ExpenseViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.example.ui.viewmodel.ExpenseViewModelFactory(
            com.example.data.repository.ExpenseRepository(
                com.example.data.local.AppDatabase.getDatabase(context).categoryDao(),
                com.example.data.local.AppDatabase.getDatabase(context).expenseDao()
            ),
            com.example.data.preferences.UserPreferencesRepository(context)
        )
    )
    val profileState by viewModel.profile.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactions.collectAsStateWithLifecycle()
    val noticesState by viewModel.notices.collectAsStateWithLifecycle()

    val profile = profileState ?: ProfileEntity()
    var showDrawer by remember { mutableStateOf(false) }

    // ------------------------------------------------------------------------
    // CALCULATIONS
    // ------------------------------------------------------------------------
    // Filter transactions to get the current month's totals
    val calNow = Calendar.getInstance()
    val thisMonth = calNow.get(Calendar.MONTH)
    val thisYear = calNow.get(Calendar.YEAR)

    var currentMonthTotalIncome = 0.0
    var currentMonthTotalExpense = 0.0

    var todayTotalIncome = 0.0
    var todayTotalExpense = 0.0

    var allTimeTotalIncome = 0.0
    var allTimeTotalExpense = 0.0

    val generalTransactions = expandTransactions(transactionsState)

    generalTransactions.forEach { tx ->
        val calTx = Calendar.getInstance().apply { timeInMillis = tx.dateTime }
        val isThisMonth = calTx.get(Calendar.YEAR) == thisYear && calTx.get(Calendar.MONTH) == thisMonth
        val isToday = isThisMonth && calTx.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)

        if (tx.type == "INCOME") {
            allTimeTotalIncome += tx.amount
            if (isThisMonth) {
                currentMonthTotalIncome += tx.amount
            }
            if (isToday) {
                todayTotalIncome += tx.amount
            }
        } else {
            allTimeTotalExpense += tx.amount
            if (isThisMonth) {
                currentMonthTotalExpense += tx.amount
            }
            if (isToday) {
                todayTotalExpense += tx.amount
            }
        }
    }

    // Include monthly salary if toggled
    val finalIncomeBudget = if (profile.isSalaryIncluded) {
        currentMonthTotalIncome + profile.monthlySalary
    } else {
        currentMonthTotalIncome
    }

    val currentBalance = profile.openingBalance +
            (if (profile.isSalaryIncluded) profile.monthlySalary else 0.0) +
            allTimeTotalIncome - allTimeTotalExpense

    val expenseRatio = if (finalIncomeBudget > 0.0) {
        (currentMonthTotalExpense / finalIncomeBudget).coerceIn(0.0, 1.0)
    } else {
        if (currentMonthTotalExpense > 0.0) 1.0 else 0.0
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Launcher for saving backup JSON file
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(viewModel.backupJsonText.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }
                Toast.makeText(context, "ব্যাকআপ ফাইল সফলভাবে সংরক্ষণ করা হয়েছে! 💾", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "ভুল হয়েছে: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Launcher for selecting backup JSON file to restore
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonText = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    coroutineScope.launch {
                        val success = viewModel.restoreFromJsonString(jsonText)
                        if (success) {
                            Toast.makeText(context, "ডাটা সফলভাবে রিস্টোর করা হয়েছে! 🔄", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "রিস্টোর করা যায়নি। দয়া করে সঠিক ব্যাকআপ ফাইল নির্বাচন করুন।", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "ফাইল পড়তে সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val showSnackbarWithUndo: (String, () -> Unit) -> Unit = { message, undoAction ->
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo ↩️",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                undoAction()
            }
        }
    }

    if (viewModel.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteConfirmDialog = false },
            title = { Text(text = viewModel.deleteConfirmTitle, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text(text = viewModel.deleteConfirmMessage, color = Color(0xFF475569), fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.pendingDeleteAction?.invoke()
                        viewModel.showDeleteConfirmDialog = false
                        viewModel.pendingDeleteAction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))
                ) {
                    Text("Yes, Delete", color = Color(0xFF0F172A))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.showDeleteConfirmDialog = false
                        viewModel.pendingDeleteAction = null
                    }
                ) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }



    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(
                activeTab = viewModel.currentTab,
                onTabSelected = { viewModel.currentTab = it }
            )
        },
        floatingActionButtonPosition = if (viewModel.currentTab == "BAZAR" && marketViewModel.isSelectionMode && marketViewModel.selectedItemIds.isNotEmpty()) {
            androidx.compose.material3.FabPosition.Center
        } else {
            androidx.compose.material3.FabPosition.End
        },
        floatingActionButton = {
            if (viewModel.currentTab == "NOTICE") {
                FloatingActionButton(
                    onClick = { viewModel.showAddNoticeDialog = true },
                    containerColor = Color(0xFF3498DB),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_note_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add New Note"
                    )
                }
            } else if (viewModel.currentTab == "ACCOUNT") {
                FloatingActionButton(
                    onClick = { viewModel.showAddPersonDialog = true },
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_person_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add New Person"
                    )
                }
            } else if (viewModel.currentTab == "TRACKER") {
                FloatingActionButton(
                    onClick = {
                        viewModel.cancelEditing()
                        viewModel.activeFormType = "EXPENSE"
                        viewModel.resetTrackerFormDateTime()
                        viewModel.showAddTransactionDialog = true
                    },
                    containerColor = Color(0xFF1976D2),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_transaction_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Transaction"
                    )
                }
            } else if (viewModel.currentTab == "BAZAR") {
                val context = LocalContext.current
                val items by marketViewModel.items.collectAsStateWithLifecycle()
                val isSelectionMode = marketViewModel.isSelectionMode
                val selectedItemIds = marketViewModel.selectedItemIds

                if (isSelectionMode && selectedItemIds.isNotEmpty()) {
                    val selectedItems = items.filter { it.id in selectedItemIds }
                    val totalPushAmount = selectedItems.filter { it.isActive }.sumOf { if (it.actualPrice > 0.0) it.actualPrice else it.targetPrice }

                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Off FAB (Slate)
                        FloatingActionButton(
                            onClick = {
                                selectedItems.forEach { item ->
                                    marketViewModel.updateItemActiveStatus(item, isActive = false)
                                }
                            },
                            containerColor = Color(0xFF64748B),
                            contentColor = Color.White,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "অফ (Off)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // On FAB (Green)
                        FloatingActionButton(
                            onClick = {
                                selectedItems.forEach { item ->
                                    marketViewModel.updateItemActiveStatus(item, isActive = true)
                                }
                            },
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "অন (On)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Push to Tracker FAB (Blue)
                        FloatingActionButton(
                            onClick = {
                                val activeSelectedItems = selectedItems.filter { it.isActive }
                                if (activeSelectedItems.isEmpty()) {
                                    Toast.makeText(context, "কোনো হিসাব অন থাকা আইটেম সিলেক্ট করা নেই!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val noteBuilder = activeSelectedItems.joinToString("") { item ->
                                        val price = if (item.actualPrice > 0.0) item.actualPrice else item.targetPrice
                                        "${item.description} (${item.quantity}) - ৳${if (price % 1.0 == 0.0) price.toInt() else price}\n"
                                    }
                                    
                                    try {
                                        viewModel.cancelEditing()
                                        viewModel.amountInput = if (totalPushAmount % 1.0 == 0.0) totalPushAmount.toInt().toString() else totalPushAmount.toString()
                                        viewModel.categoryInput = "বাজার"
                                        viewModel.noteInput = noteBuilder
                                        viewModel.activeFormType = "EXPENSE"
                                        viewModel.selectedPersonName = "General"
                                        viewModel.resetTrackerFormDateTime()
                                        
                                        activeSelectedItems.forEach { item ->
                                            if (item.actualPrice == 0.0) {
                                                marketViewModel.updateActualPrice(item, item.targetPrice)
                                            }
                                        }
                                        
                                        viewModel.currentTab = "TRACKER"
                                        viewModel.showAddTransactionDialog = true
                                        
                                        Toast.makeText(context, "ট্রেকার ডায়লগে বাজার তালিকা যুক্ত করা হয়েছে! 🛒", Toast.LENGTH_LONG).show()
                                        
                                        marketViewModel.isSelectionMode = false
                                        marketViewModel.selectedItemIds = emptySet()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "ত্রুটি: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "ট্রেকারে যুক্ত (৳${convertToBengaliNumber(String.format(java.util.Locale.US, "%,.0f", totalPushAmount))})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    FloatingActionButton(
                        onClick = { marketViewModel.showAddItemDialog = true },
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White,
                        modifier = Modifier.testTag("add_bazar_item_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Bazar Item"
                        )
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = Color(0xFF1E1E24),
                        contentColor = Color.White,
                        actionColor = Color(0xFF2ECC71),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        var mainDragX by remember { mutableStateOf(0f) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { mainDragX = 0f },
                        onDragEnd = {
                            if (mainDragX < -100f) {
                                showDrawer = true
                            }
                        },
                        onDragCancel = { mainDragX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            mainDragX += dragAmount
                            if (mainDragX < -100f) {
                                showDrawer = true
                            }
                        }
                    )
                }
        ) {
            // Main Content Area with scroll state
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Header
                AppHeader(
                    viewModel = viewModel,
                    onBackupClick = {
                        viewModel.backupJsonText = viewModel.generateBackupJsonString()
                        createDocumentLauncher.launch("finance_backup.json")
                    },
                    onRestoreClick = {
                        openDocumentLauncher.launch(arrayOf("*/*"))
                    },
                    showDrawer = showDrawer,
                    onShowDrawerChange = { showDrawer = it }
                )

                // Animated views based on tab choice with horizontal padding
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AnimatedContent(
                        targetState = viewModel.currentTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "tabChange"
                    ) { targetTab ->
                        when (targetTab) {
                            "ACCOUNT" -> {
                                val personsState by viewModel.persons.collectAsStateWithLifecycle()
                                val filteredPersons = remember(personsState) {
                                    personsState.filter { !it.name.trim().equals("সাধারণ", ignoreCase = true) && !it.name.trim().equals("general", ignoreCase = true) && it.name.trim().isNotEmpty() }
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    DuesLedgerSessionView(
                                        viewModel = viewModel,
                                        transactions = transactionsState,
                                        persons = filteredPersons,
                                        showSnackbarWithUndo = showSnackbarWithUndo
                                    )
                                    Spacer(modifier = Modifier.height(40.dp))
                                }
                            }
                            "TRACKER" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    TrackerSessionView(
                                        viewModel = viewModel,
                                        transactions = transactionsState,
                                        thisMonth = thisMonth,
                                        thisYear = thisYear,
                                        showSnackbarWithUndo = showSnackbarWithUndo
                                    )
                                    Spacer(modifier = Modifier.height(40.dp))
                                }
                            }
                            "NOTICE" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    NoticeSessionView(
                                        viewModel = viewModel,
                                        notices = noticesState,
                                        showSnackbarWithUndo = showSnackbarWithUndo
                                    )
                                    Spacer(modifier = Modifier.height(40.dp))
                                }
                            }
                            "BAZAR" -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    com.example.ui.MarketApp(viewModel = marketViewModel, financeViewModel = viewModel)
                                }
                            }
                            "BUDGET" -> {
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    com.example.ui.BudgetTabContent(viewModel = expenseViewModel)
                                }
                            }
                        }
                    }
                }
            }

            // Hidden Note Double Click Popup Dialog
            viewModel.selectedTransactionForDetails?.let { tx ->
                DoubleTapDetailsDialog(
                    transaction = tx,
                    onDismiss = { viewModel.selectedTransactionForDetails = null }
                )
            }
        }
    }
}

// ============================================================================
// HEADER VIEW
// ============================================================================

fun getCertificateFingerprint(context: Context, type: String): String {
    try {
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
        }
        
        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        
        if (signatures != null && signatures.isNotEmpty()) {
            val signature = signatures[0]
            val md = MessageDigest.getInstance(type)
            val digest = md.digest(signature.toByteArray())
            return digest.joinToString(":") { String.format("%02X", it) }
        }
    } catch (e: Exception) {
        return e.localizedMessage ?: "Error"
    }
    return "Not Found"
}

@Composable
fun CredentialRow(label: String, value: String, context: Context) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                fontSize = 10.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = Color(0xFF0F172A),
                modifier = Modifier.weight(1f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = {
                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clipData = android.content.ClipData.newPlainText(label, value)
                    clipboardManager.setPrimaryClip(clipData)
                    Toast.makeText(context, "$label কপি করা হয়েছে! 📋", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AppHeader(
    viewModel: FinanceViewModel,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    showDrawer: Boolean,
    onShowDrawerChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val currentUser = viewModel.currentUserState
    var showAuthDialog by remember { mutableStateOf(false) }
    var showCredentials by remember { mutableStateOf(false) }

    // Google Sign-In setup
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("989102272624-n5gu65hmac1t1r9fh4f9bd4cubp7uuvp.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                try {
                    initFirebase(context)
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener { authResultTask ->
                            if (authResultTask.isSuccessful) {
                                Toast.makeText(context, "গুগল সাইন-ইন সফল হয়েছে! 🎉", Toast.LENGTH_SHORT).show()
                                showAuthDialog = false
                            } else {
                                Toast.makeText(context, "সাইন-ইন ব্যর্থ হয়েছে: ${authResultTask.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } catch (e: Throwable) {
                    Toast.makeText(context, "গুগল সাইন-ইন সার্ভিস সাময়িকভাবে অলভ্য: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "ভুল হয়েছে: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = {
                Text(
                    text = "প্রোফাইল ও অ্যাপ তথ্য ℹ️",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile picture
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (currentUser != null) Color(0xFF2E7D32) else Color(0xFF64748B)),
                        contentAlignment = Alignment.Center
                    ) {
                        val photoUrl = currentUser?.photoUrl?.toString()
                        if (photoUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(photoUrl),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val initial = currentUser?.displayName?.firstOrNull()?.uppercase() ?: "G"
                            Text(
                                text = initial,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            )
                        }
                    }

                    // Profile Name
                    Text(
                        text = currentUser?.displayName ?: "Guest User",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                    // App Information Text
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "অ্যাপ সম্পর্কে তথ্য 📱",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Meneger2.0 হলো একটি আধুনিক অল-ইন-ওয়ান ফাইন্যান্স ট্র্যাকার, বাজার তালিকা ও ডায়েরি অ্যাপ। এটি আপনাকে আপনার দৈনিক আয়-ব্যয় হিসাব করতে, দেনা-পাওনা লেজার বুক বজায় রাখতে, বাজারের তালিকা সাজাতে এবং নোটসমূহ লিখে রাখতে সাহায্য করে।",
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "প্রধান ফিচারসমূহ ✨:\n" +
                                    "• ক্লাউড এবং লোকাল জেসন (JSON) ডাটা ব্যাকআপ ও রিস্টোর ব্যবস্থা\n" +
                                    "• বাজারের তালিকা (Bazaar List) ও আইটেম উপরে/নিচে সাজানোর (Reorder) মোড\n" +
                                    "• ট্র্যাকার সামারিতে আয়-ব্যয় ফিল্টারিং সুবিধা\n" +
                                    "• সহজ দেনা-পাওনা লেজার বুক\n" +
                                    "• ক্যাটাগরি অনুসারে বাজেট ট্র্যাকার\n" +
                                    "• ডায়েরি/নোটবুক ব্যবহারের চমৎকার অভিজ্ঞতা",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAuthDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ঠিক আছে", color = Color.White, fontSize = 12.sp)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2563EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable { showAuthDialog = true }
            ) {
                // Circle Profile Pic or H in circle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentUser != null) Color(0xFF2E7D32) else Color(0xFF2563EB)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentUser != null) {
                        val photoUrl = currentUser.photoUrl?.toString()
                        if (photoUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(photoUrl),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val initial = currentUser.displayName?.firstOrNull()?.uppercase() ?: "H"
                            Text(
                                text = initial,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Text(
                            text = "H",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Meneger2.0 💰",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = if (currentUser != null) (currentUser.displayName ?: "") else "All-in-One Personal Dashboard",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(
                onClick = { onShowDrawerChange(true) }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu Options",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    if (showDrawer) {
        Dialog(
            onDismissRequest = { onShowDrawerChange(false) },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            var drawerDragX by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onShowDrawerChange(false) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { drawerDragX = 0f },
                            onDragEnd = {
                                if (drawerDragX > 100f) {
                                    onShowDrawerChange(false)
                                }
                            },
                            onDragCancel = { drawerDragX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                drawerDragX += dragAmount
                                if (drawerDragX > 100f) {
                                    onShowDrawerChange(false)
                                }
                            }
                        )
                    }
            ) {
                var animatedState by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    animatedState = true
                }

                AnimatedVisibility(
                    visible = animatedState,
                    enter = slideInHorizontally(
                        initialOffsetX = { it }
                    ),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it }
                    ),
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(300.dp)
                        .align(Alignment.CenterEnd)
                        .clickable(enabled = false) { }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { drawerDragX = 0f },
                                onDragEnd = {
                                    if (drawerDragX > 100f) {
                                        onShowDrawerChange(false)
                                    }
                                },
                                onDragCancel = { drawerDragX = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    drawerDragX += dragAmount
                                    if (drawerDragX > 100f) {
                                        onShowDrawerChange(false)
                                    }
                                }
                            )
                        }
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Meneger2.0 অপশনসমূহ ⚙️",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    IconButton(onClick = { onShowDrawerChange(false) }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close Drawer",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Divider(color = Color(0xFFE2E8F0), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                                Text(
                                    text = "ডাটা ব্যাকআপ ও রিস্টোর 🔄",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            onShowDrawerChange(false)
                                            onBackupClick()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2).copy(alpha = 0.12f), contentColor = Color(0xFF1976D2)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.3f)),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Backup,
                                                contentDescription = "Backup",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text("ব্যাকআপ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onShowDrawerChange(false)
                                            onRestoreClick()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.12f), contentColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f)),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Restore,
                                                contentDescription = "Restore",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text("রিস্টোর", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onShowDrawerChange(false)
                                            if (currentUser != null) {
                                                viewModel.syncFromCloud(currentUser.uid)
                                                Toast.makeText(context, "ক্লাউড থেকে ডাটা সিঙ্ক হচ্ছে... 🔄", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "সিঙ্ক করতে দয়া করে প্রথমে গুগল দিয়ে লগইন করুন।", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.12f), contentColor = Color(0xFFE65100)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f)),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = "Sync",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text("Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Divider(color = Color(0xFFE2E8F0), thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "অ্যাপ ক্রেডেনশিয়ালস 🔑",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    TextButton(
                                        onClick = { showCredentials = !showCredentials },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = if (showCredentials) "লুকান 🔼" else "দেখুন 🔽",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1976D2)
                                        )
                                    }
                                }

                                if (showCredentials) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        val packageName = context.packageName
                                        val sha1 = remember { getCertificateFingerprint(context, "SHA-1") }
                                        val sha256 = remember { getCertificateFingerprint(context, "SHA-256") }

                                        CredentialRow(label = "Package Name", value = packageName, context = context)
                                        Divider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                                        CredentialRow(label = "SHA-1 Fingerprint", value = sha1, context = context)
                                        Divider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                                        CredentialRow(label = "SHA-256 Fingerprint", value = sha256, context = context)
                                    }
                                }
                            }

                            // Bottom Login / Logout Button - pinned to bottom, strictly NO emojis or icons
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    onShowDrawerChange(false)
                                    if (currentUser != null) {
                                        viewModel.logoutAndClearLocal {
                                            googleSignInClient.signOut().addOnCompleteListener {
                                                Toast.makeText(context, "লগআউট করা হয়েছে এবং লোকাল ডাটা ক্লিয়ার করা হয়েছে।", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentUser != null) Color(0xFFC62828) else Color(0xFF1976D2)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text(
                                    text = if (currentUser != null) "লগআউট করুন" else "গুগল দিয়ে লগইন",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SESSION 1: DUES LEDGER SESSION VIEW (replaces Account Session View)
// ============================================================================

data class Repayment(val amount: Double, val timestamp: Long)

fun parseRepayments(repaymentsCsv: String): List<Repayment> {
    if (repaymentsCsv.trim().isEmpty()) return emptyList()
    return repaymentsCsv.split(";").mapNotNull {
        val parts = it.split("|")
        if (parts.size == 2) {
            val amt = parts[0].toDoubleOrNull()
            val ts = parts[1].toLongOrNull()
            if (amt != null && ts != null) {
                Repayment(amt, ts)
            } else null
        } else null
    }
}

fun formatRepayments(repayments: List<Repayment>): String {
    return repayments.joinToString(";") { "${it.amount}|${it.timestamp}" }
}

fun expandTransactions(txList: List<TransactionEntity>): List<TransactionEntity> {
    val result = ArrayList<TransactionEntity>()
    for (tx in txList) {
        result.add(tx)
        val isPersonal = tx.isPersonal
        if (isPersonal && tx.repaymentsCsv.trim().isNotEmpty()) {
            val repayments = parseRepayments(tx.repaymentsCsv)
            for (rep in repayments) {
                val syntheticType = if (tx.type == "EXPENSE") "INCOME" else "EXPENSE"
                val mainDetails = if (tx.note.isNotBlank()) tx.note else tx.category
                val syntheticNote = "Repayment: $mainDetails"
                result.add(
                    TransactionEntity(
                        id = -tx.id - rep.timestamp.toInt().coerceAtLeast(1),
                        amount = rep.amount,
                        type = syntheticType,
                        category = "Account",
                        dateTime = rep.timestamp,
                        note = syntheticNote,
                        personName = tx.personName,
                        paidAmount = 0.0,
                        repaymentsCsv = "",
                        isPersonal = true
                    )
                )
            }
        }
    }
    return result
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DuesLedgerSessionView(
    viewModel: FinanceViewModel,
    transactions: List<TransactionEntity>,
    persons: List<PersonEntity>,
    showSnackbarWithUndo: (String, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var expandedPersonId by remember { mutableStateOf<Int?>(null) }

    var showEditPersonDialog by remember { mutableStateOf(false) }
    var personToEdit by remember { mutableStateOf<PersonEntity?>(null) }
    var editPersonNameInput by remember { mutableStateOf("") }

    var showAddTxDialogForPerson by remember { mutableStateOf<PersonEntity?>(null) }
    var showAddRepaymentDialogForTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var showEditTransactionAndRepaymentsDialog by remember { mutableStateOf<TransactionEntity?>(null) }

    data class PersonDuesModel(
        val person: PersonEntity,
        val totalGiven: Double,
        val totalTaken: Double,
        val netBalance: Double,
        val txCount: Int
    )

    // Calculations across all people
    var totalReceivable = 0.0
    var totalPayable = 0.0

    val personDuesList = persons.map { person ->
        val personTx = transactions.filter { it.isPersonal && it.personName.trim().lowercase() == person.name.trim().lowercase() }
        val incomeFromPerson = personTx.filter { it.type == "INCOME" }.sumOf { maxOf(0.0, it.amount - it.paidAmount) }
        val expenseToPerson = personTx.filter { it.type == "EXPENSE" }.sumOf { maxOf(0.0, it.amount - it.paidAmount) }
        val netBalance = expenseToPerson - incomeFromPerson

        totalReceivable += expenseToPerson
        totalPayable += incomeFromPerson

        PersonDuesModel(
            person = person,
            totalGiven = expenseToPerson,
            totalTaken = incomeFromPerson,
            netBalance = netBalance,
            txCount = personTx.size
        )
    }

    val netStanding = totalReceivable - totalPayable

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 1. BIG SUMMARY CARD FOR DUES (Budget Style)
        val netStandingAbs = if (netStanding >= 0.0) netStanding else -netStanding
        val netPrefix = if (netStanding >= 0.0) "৳ " else "-৳ "
        val totalDuesVolume = (totalReceivable + totalPayable).coerceAtLeast(1.0)
        val receivableProgress = (totalReceivable / totalDuesVolume).toFloat().coerceIn(0f, 1f)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0060A8)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "নিট স্ট্যাটাস (Net Status)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = netPrefix + convertToBengaliNumber(String.format(Locale.US, "%,.0f", netStandingAbs)),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👥", fontSize = 18.sp)
                    }
                }

                // Middle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "পাওনা (Receivable)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "৳ " + convertToBengaliNumber(String.format(Locale.US, "%,.0f", totalReceivable)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF86EFAC)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "দেনা (Payable)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "৳ " + convertToBengaliNumber(String.format(Locale.US, "%,.0f", totalPayable)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }

                // Bottom Progress Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পাওনা অনুপাত",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "${(receivableProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { receivableProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF86EFAC),
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
            }
        }

        // 3. INDIVIDUAL DUES LIST
        Text(
            text = "📋 People Dues List",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            modifier = Modifier.padding(top = 4.dp)
        )

        if (persons.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("👥", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No people found!",
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Click the floating action button (+) at the bottom right corner to add a new person to start tracking individual debts and dues.",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                personDuesList.forEach { model ->
                    val person = model.person
                    val totalGiven = model.totalGiven
                    val totalTaken = model.totalTaken
                    val netBalance = model.netBalance
                    val txCount = model.txCount

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (netBalance > 0) Color(0xFF2E7D32).copy(alpha = 0.25f) else if (netBalance < 0) Color(0xFFC62828).copy(alpha = 0.25f) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            expandedPersonId = if (expandedPersonId == person.id) null else person.id
                                        },
                                        onLongClick = {
                                            viewModel.confirmDelete(
                                                title = "Delete Person Confirmation",
                                                message = "Do you want to delete ${person.name} and all of their transactions?",
                                                action = {
                                                    viewModel.deletePersonWithUndo(person, showSnackbarWithUndo)
                                                }
                                            )
                                        }
                                    )
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    // Avatar circle with first letter
                                    val avatarChar = if (person.name.trim().isNotEmpty()) person.name.trim()[0].toString() else "?"
                                    val avatarBgColor = if (netBalance > 0) Color(0xFF2E7D32).copy(alpha = 0.12f) else if (netBalance < 0) Color(0xFFC62828).copy(alpha = 0.12f) else Color(0xFFF1F5F9)
                                    val avatarTextColor = if (netBalance > 0) Color(0xFF2E7D32) else if (netBalance < 0) Color(0xFFC62828) else Color(0xFF0F172A)
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(avatarBgColor)
                                    ) {
                                        Text(
                                            text = avatarChar,
                                            color = avatarTextColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = person.name,
                                            color = Color(0xFF0F172A),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val netText = if (netBalance > 0) {
                                            "Net: You will receive ৳" + convertToBengaliNumber(String.format(Locale.US, "%,.0f", netBalance))
                                        } else if (netBalance < 0) {
                                            "Net: You owe ৳" + convertToBengaliNumber(String.format(Locale.US, "%,.0f", -netBalance))
                                        } else {
                                            "Net: Balanced"
                                        }
                                        val netColor = if (netBalance > 0) Color(0xFF2E7D32) else if (netBalance < 0) Color(0xFFC62828) else Color(0xFF64748B)
                                        Text(
                                            text = netText,
                                            color = netColor,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                // Edit button
                                if (!person.name.trim().equals("সাধারণ", ignoreCase = true) && !person.name.trim().equals("general", ignoreCase = true)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1976D2).copy(alpha = 0.08f))
                                            .clickable {
                                                personToEdit = person
                                                editPersonNameInput = person.name
                                                showEditPersonDialog = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✏️", fontSize = 10.sp)
                                    }
                                }
                            }

                            // EXPANDED SECTION WITH INDIVIDUAL TRANSACTION DETAILS
                            if (expandedPersonId == person.id) {
                                Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // "Pressing a person's name shows a button to add a new transaction"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Button(
                                            onClick = { showAddTxDialogForPerson = person },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("➕ Add New Transaction", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Transaction history list for this person
                                    val personTx = transactions.filter { it.isPersonal && it.personName.trim().lowercase() == person.name.trim().lowercase() }
                                        .sortedByDescending { it.dateTime }

                                    Text(
                                        text = "📜 Transaction History (" + convertToBengaliNumber(personTx.size.toString()) + "):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )

                                    if (personTx.isEmpty()) {
                                        Text(
                                            text = "No previous transactions.",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            personTx.forEach { tx ->
                                                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(tx.dateTime))
                                                val isInc = tx.type == "INCOME"
                                                val prefix = if (isInc) "+" else "-"
                                                val color = if (isInc) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                
                                                val repayments = parseRepayments(tx.repaymentsCsv)
                                                val totalRepaymentsAmount = repayments.sumOf { it.amount }
                                                val remaining = maxOf(0.0, tx.amount - totalRepaymentsAmount)
                                                val isFullyPaid = totalRepaymentsAmount >= tx.amount

                                                // Card layout representing one transaction block
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .combinedClickable(
                                                            onClick = {
                                                                // Single click does nothing or triggers details, let's keep it safe
                                                            },
                                                            onDoubleClick = {
                                                                // "লিস্টে ডাবল টেপ একটা পপাপ উইন্ডো তে বিবরন তারিখ ও টাকা ও প্রতেক্টা পরিশোধ তারিখ ও টাকা এডিট করা যাবে"
                                                                showEditTransactionAndRepaymentsDialog = tx
                                                            },
                                                            onLongClick = {
                                                                // "লং প্রেস ডিলেট, আর ডিলেট বাটন বাদ"
                                                                viewModel.confirmDelete(
                                                                    title = "Delete Transaction Confirmation",
                                                                    message = "Are you sure you want to delete this transaction?",
                                                                    action = {
                                                                        viewModel.deleteTransactionWithUndo(tx, showSnackbarWithUndo)
                                                                    }
                                                                )
                                                            }
                                                        ),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        // Line 1: বিবরন তারিখ-সময় টাকা
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Text(
                                                                    text = tx.category,
                                                                    color = Color(0xFF0F172A),
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                Text(
                                                                    text = convertToBengaliNumber(dateStr),
                                                                    color = Color(0xFF64748B),
                                                                    fontSize = 8.sp
                                                                )
                                                            }
                                                            Text(
                                                                text = "$prefix ৳" + convertToBengaliNumber(String.format(Locale.US, "%,.0f", tx.amount)),
                                                                color = color,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }

                                                        // Line 2+: repayments listed on their own line
                                                        repayments.forEachIndexed { repIdx, rep ->
                                                            val repDateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(rep.timestamp))
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(top = 4.dp, start = 8.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "Repaid",
                                                                        color = Color(0xFF1976D2),
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Medium
                                                                    )
                                                                    Text(
                                                                        text = convertToBengaliNumber(repDateStr),
                                                                        color = Color(0xFF64748B),
                                                                        fontSize = 8.sp
                                                                    )
                                                                }
                                                                Text(
                                                                    text = "৳" + convertToBengaliNumber(String.format(Locale.US, "%,.0f", rep.amount)),
                                                                    color = Color(0xFF475569),
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            }
                                                        }

                                                        Divider(color = Color(0xFFF1F5F9), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))

                                                        // Bottom Line: পরিশোধ যোগ(বাটন) বাকী: <amount> টাকা
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Button(
                                                                onClick = { showAddRepaymentDialogForTx = tx },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                                                shape = RoundedCornerShape(6.dp),
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                modifier = Modifier.height(24.dp)
                                                            ) {
                                                                Text("Add Pay ➕", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                            }

                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Remaining:",
                                                                    color = Color(0xFF64748B),
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                                val remainingColor = if (isFullyPaid) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                                val remainingWeight = if (isFullyPaid) FontWeight.Normal else FontWeight.Bold
                                                                val remainingText = if (isFullyPaid) "Fully Paid ✅" else "৳" + convertToBengaliNumber(String.format(Locale.US, "%,.0f", remaining))
                                                                Text(
                                                                    text = remainingText,
                                                                    color = remainingColor,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = remainingWeight
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // POPUP DIALOGS
    // ------------------------------------------------------------------------

    // Add Person Dialog (triggered from FAB in Parent Scaffold)
    if (viewModel.showAddPersonDialog) {
        Dialog(onDismissRequest = { viewModel.showAddPersonDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "👥 Add New Person",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    OutlinedTextField(
                        value = viewModel.newPersonNameInput,
                        onValueChange = { viewModel.newPersonNameInput = it },
                        placeholder = { Text("Enter name...", fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF2E7D32),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.showAddPersonDialog = false }) {
                            Text("Cancel", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (viewModel.newPersonNameInput.trim().isNotEmpty()) {
                                    viewModel.addPerson()
                                    viewModel.showAddPersonDialog = false
                                    Toast.makeText(context, "New person added successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a valid name!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Save 💾", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Add Transaction for specific person Dialog
    if (showAddTxDialogForPerson != null) {
        val person = showAddTxDialogForPerson!!
        var quickAmountInput by remember { mutableStateOf("") }
        var quickCategoryInput by remember { mutableStateOf("") }
        var quickDateTime by remember { mutableStateOf(System.currentTimeMillis()) }

        val calendar = remember { Calendar.getInstance() }
        val dateSdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
        var dateTimeString by remember { mutableStateOf(dateSdf.format(Date(quickDateTime))) }

        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.timeInMillis = quickDateTime
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                android.app.TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        quickDateTime = calendar.timeInMillis
                        dateTimeString = dateSdf.format(Date(quickDateTime))
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        Dialog(
            onDismissRequest = { showAddTxDialogForPerson = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 580.dp)
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "➕ New Transaction (${person.name})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    OutlinedTextField(
                        value = quickAmountInput,
                        onValueChange = { quickAmountInput = it },
                        placeholder = { Text("Amount ৳", fontSize = 14.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    OutlinedTextField(
                        value = quickCategoryInput,
                        onValueChange = { quickCategoryInput = it },
                        placeholder = { Text("Description (e.g. loan, payment)", fontSize = 14.sp) },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )

                    // Date & Time selection field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                calendar.timeInMillis = quickDateTime
                                datePickerDialog.show()
                            }
                    ) {
                        OutlinedTextField(
                            value = convertToBengaliNumber(dateTimeString),
                            onValueChange = { },
                            readOnly = true,
                            enabled = false,
                            label = { Text("তারিখ ও সময় (Date & Time)", fontSize = 11.sp) },
                            trailingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date & Time", tint = Color(0xFF1976D2))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color(0xFF0F172A),
                                disabledBorderColor = Color(0xFFCBD5E1),
                                disabledLabelColor = Color(0xFF64748B)
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val amt = quickAmountInput.toDoubleOrNull() ?: 0.0
                                if (amt > 0.0) {
                                    val desc = quickCategoryInput.trim().ifEmpty { "Gave Money" }
                                    viewModel.viewModelScope.launch {
                                        viewModel.dao.insertTransaction(
                                            TransactionEntity(
                                                amount = amt,
                                                type = "EXPENSE",
                                                category = "Account",
                                                dateTime = quickDateTime,
                                                note = desc,
                                                personName = person.name,
                                                isPersonal = true
                                            )
                                        )
                                    }
                                    showAddTxDialogForPerson = null
                                    Toast.makeText(context, "Transaction saved successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a valid amount!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("I Gave 📉", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                val amt = quickAmountInput.toDoubleOrNull() ?: 0.0
                                if (amt > 0.0) {
                                    val desc = quickCategoryInput.trim().ifEmpty { "Received Money" }
                                    viewModel.viewModelScope.launch {
                                        viewModel.dao.insertTransaction(
                                            TransactionEntity(
                                                amount = amt,
                                                type = "INCOME",
                                                category = "Account",
                                                dateTime = quickDateTime,
                                                note = desc,
                                                personName = person.name,
                                                isPersonal = true
                                            )
                                        )
                                    }
                                    showAddTxDialogForPerson = null
                                    Toast.makeText(context, "Transaction saved successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a valid amount!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("I Got 📈", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // Add Repayment Dialog
    if (showAddRepaymentDialogForTx != null) {
        val tx = showAddRepaymentDialogForTx!!
        var repaymentAmountInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddRepaymentDialogForTx = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "💸 Add New Repayment",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    OutlinedTextField(
                        value = repaymentAmountInput,
                        onValueChange = { repaymentAmountInput = it },
                        placeholder = { Text("Amount ৳", fontSize = 14.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF2E7D32),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddRepaymentDialogForTx = null }) {
                            Text("Cancel", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                val amt = repaymentAmountInput.toDoubleOrNull() ?: 0.0
                                if (amt > 0.0) {
                                    val repayments = parseRepayments(tx.repaymentsCsv).toMutableList()
                                    repayments.add(Repayment(amt, System.currentTimeMillis()))
                                    val updatedCsv = formatRepayments(repayments)
                                    val totalPaid = repayments.sumOf { it.amount }

                                    viewModel.viewModelScope.launch {
                                        viewModel.dao.updateTransaction(
                                            tx.copy(
                                                repaymentsCsv = updatedCsv,
                                                paidAmount = totalPaid
                                            )
                                        )
                                    }
                                    showAddRepaymentDialogForTx = null
                                    Toast.makeText(context, "Repayment added successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a valid amount!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Save", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Edit Transaction and Repayments Dialog
    if (showEditTransactionAndRepaymentsDialog != null) {
        val tx = showEditTransactionAndRepaymentsDialog!!
        var categoryInput by remember { mutableStateOf(tx.category) }
        var amountInput by remember { mutableStateOf(tx.amount.toString()) }
        var dateTimeInput by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(tx.dateTime))) }
        var typeInput by remember { mutableStateOf(tx.type) }

        val initialRepayments = remember(tx.repaymentsCsv) { parseRepayments(tx.repaymentsCsv) }
        var repaymentsState by remember { mutableStateOf(initialRepayments) }

        Dialog(
            onDismissRequest = { showEditTransactionAndRepaymentsDialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 580.dp)
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "✏️ Edit Transaction & Repayments",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { typeInput = "EXPENSE" },
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (typeInput == "EXPENSE") Color(0xFFC62828) else Color(0xFFF1F5F9),
                                    contentColor = if (typeInput == "EXPENSE") Color.White else Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("I Gave 📉", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }

                            Button(
                                onClick = { typeInput = "INCOME" },
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (typeInput == "INCOME") Color(0xFF2E7D32) else Color(0xFFF1F5F9),
                                    contentColor = if (typeInput == "INCOME") Color.White else Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("I Got 📈", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }

                        OutlinedTextField(
                            value = categoryInput,
                            onValueChange = { categoryInput = it },
                            label = { Text("Description", fontSize = 11.sp) },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )

                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Main Amount ৳", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )

                        val calendar = Calendar.getInstance()
                        if (dateTimeInput.isNotEmpty()) {
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                val parsedDate = sdf.parse(dateTimeInput)
                                if (parsedDate != null) {
                                    calendar.time = parsedDate
                                }
                            } catch (e: Exception) {}
                        }

                        val datePickerDialog = android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        calendar.set(Calendar.MINUTE, minute)
                                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                        dateTimeInput = sdf.format(calendar.time)
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() }
                        ) {
                            OutlinedTextField(
                                value = dateTimeInput,
                                onValueChange = { },
                                readOnly = true,
                                enabled = false,
                                label = { Text("Date (YYYY-MM-DD HH:MM)", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color(0xFF0F172A),
                                    disabledBorderColor = Color(0xFFCBD5E1),
                                    disabledPlaceholderColor = Color(0xFF64748B)
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )
                        }

                        if (repaymentsState.isNotEmpty()) {
                            Text(
                                text = "💸 Repayments List:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )

                            repaymentsState.forEachIndexed { index, repayment ->
                                var repAmt by remember(repayment) { mutableStateOf(repayment.amount.toString()) }
                                var repDate by remember(repayment) { mutableStateOf(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(repayment.timestamp))) }

                                val repCalendar = Calendar.getInstance().apply { timeInMillis = repayment.timestamp }
                                val repDatePickerDialog = android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        repCalendar.set(Calendar.YEAR, year)
                                        repCalendar.set(Calendar.MONTH, month)
                                        repCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                repCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                repCalendar.set(Calendar.MINUTE, minute)
                                                val ts = repCalendar.timeInMillis
                                                repaymentsState = repaymentsState.toMutableList().apply {
                                                    this[index] = Repayment(repaymentsState[index].amount, ts)
                                                }
                                                repDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
                                            },
                                            repCalendar.get(Calendar.HOUR_OF_DAY),
                                            repCalendar.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    },
                                    repCalendar.get(Calendar.YEAR),
                                    repCalendar.get(Calendar.MONTH),
                                    repCalendar.get(Calendar.DAY_OF_MONTH)
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = repAmt,
                                            onValueChange = {
                                                repAmt = it
                                                val dVal = it.toDoubleOrNull() ?: 0.0
                                                repaymentsState = repaymentsState.toMutableList().apply {
                                                    this[index] = Repayment(dVal, repaymentsState[index].timestamp)
                                                }
                                            },
                                            label = { Text("৳", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color(0xFF0F172A),
                                                unfocusedTextColor = Color(0xFF0F172A),
                                                focusedBorderColor = Color(0xFF1976D2),
                                                unfocusedBorderColor = Color(0xFFCBD5E1)
                                            ),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .weight(1.8f)
                                                .clickable { repDatePickerDialog.show() }
                                        ) {
                                            OutlinedTextField(
                                                value = repDate,
                                                onValueChange = { },
                                                readOnly = true,
                                                enabled = false,
                                                label = { Text("Date", fontSize = 10.sp) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    disabledTextColor = Color(0xFF0F172A),
                                                    disabledBorderColor = Color(0xFFCBD5E1),
                                                    disabledPlaceholderColor = Color(0xFF64748B)
                                                ),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                repaymentsState = repaymentsState.toMutableList().apply {
                                                    removeAt(index)
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("🗑️", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditTransactionAndRepaymentsDialog = null }) {
                            Text("Cancel", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                val finalAmt = amountInput.toDoubleOrNull() ?: tx.amount
                                val finalCategory = categoryInput.trim().ifEmpty { tx.category }
                                val finalTime = try {
                                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(dateTimeInput)?.time ?: tx.dateTime
                                } catch (e: Exception) {
                                    tx.dateTime
                                }

                                val filteredRepayments = repaymentsState.filter { it.amount > 0.0 }
                                val updatedCsv = formatRepayments(filteredRepayments)
                                val finalPaid = filteredRepayments.sumOf { it.amount }

                                viewModel.viewModelScope.launch {
                                    viewModel.dao.updateTransaction(
                                        tx.copy(
                                            amount = finalAmt,
                                            type = typeInput,
                                            category = finalCategory,
                                            dateTime = finalTime,
                                            repaymentsCsv = updatedCsv,
                                            paidAmount = finalPaid
                                        )
                                    )
                                }
                                showEditTransactionAndRepaymentsDialog = null
                                Toast.makeText(context, "Transaction updated successfully!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Save", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Person Name Editor Dialog
    if (showEditPersonDialog && personToEdit != null) {
        AlertDialog(
            onDismissRequest = {
                showEditPersonDialog = false
                personToEdit = null
            },
            title = { Text("Edit Name", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                OutlinedTextField(
                    value = editPersonNameInput,
                    onValueChange = { editPersonNameInput = it },
                    placeholder = { Text("Enter new name", fontSize = 14.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF1976D2),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = editPersonNameInput.trim()
                        if (newName.isNotEmpty()) {
                            viewModel.updatePersonName(personToEdit!!, newName)
                            showEditPersonDialog = false
                            personToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Save", color = Color.White, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditPersonDialog = false
                        personToEdit = null
                    }
                ) {
                    Text("Cancel", color = Color(0xFF64748B), fontSize = 11.sp)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

fun convertToBengaliNumber(numberStr: String): String {
    return numberStr
}

// ============================================================================
// DAILY SUMMARY MODEL
// ============================================================================
data class DailySummaryModel(
    val dateString: String,
    val timestamp: Long,
    var income: Double = 0.0,
    var expense: Double = 0.0,
    var balanceAtEnd: Double = 0.0
)

@Composable
fun TrackerSessionView(
    viewModel: FinanceViewModel,
    transactions: List<TransactionEntity>,
    thisMonth: Int,
    thisYear: Int,
    showSnackbarWithUndo: (String, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val profileState by viewModel.profile.collectAsStateWithLifecycle()
    val profile = profileState ?: ProfileEntity()

    val personsState by viewModel.persons.collectAsStateWithLifecycle(initialValue = emptyList())

    // Suggestions logic for Category and Group/Person inputs
    val allCategories = remember(transactions) {
        transactions.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    val allGroups = remember(transactions, personsState) {
        val fromTxs = transactions.map { it.personName.trim() }
        val fromPersons = personsState.map { it.name.trim() }
        (fromTxs + fromPersons + listOf("General", "সাধারণ")).filter { it.isNotEmpty() && !it.equals("General", ignoreCase = true) && !it.equals("সাধারণ", ignoreCase = true) }.distinct()
    }

    val filteredCategorySuggestions = remember(allCategories, viewModel.categoryInput) {
        if (viewModel.categoryInput.isEmpty()) {
            allCategories.take(5)
        } else {
            allCategories.filter { it.contains(viewModel.categoryInput, ignoreCase = true) && !it.equals(viewModel.categoryInput, ignoreCase = true) }.take(5)
        }
    }

    val filteredGroupSuggestions = remember(allGroups, viewModel.selectedPersonName) {
        if (viewModel.selectedPersonName.isEmpty()) {
            allGroups.take(5)
        } else {
            allGroups.filter { it.contains(viewModel.selectedPersonName, ignoreCase = true) && !it.equals(viewModel.selectedPersonName, ignoreCase = true) }.take(5)
        }
    }

    var dialogType by remember { mutableStateOf("EXPENSE") } // "INCOME" or "EXPENSE"

    // Sync dialogType with activeFormType when dialog is shown
    LaunchedEffect(viewModel.showAddTransactionDialog) {
        if (viewModel.showAddTransactionDialog) {
            dialogType = viewModel.activeFormType
        }
    }

    // If we enter edit mode from somewhere, automatically show the dialog
    LaunchedEffect(viewModel.editingTransactionId) {
        if (viewModel.editingTransactionId != null) {
            dialogType = viewModel.activeFormType
            viewModel.showAddTransactionDialog = true
        }
    }

    val generalTransactions = expandTransactions(transactions)

    // 1. Calculate Carry-over Balance from previous months if month is filtered
    val isMonthFiltered = viewModel.filterYear != "ALL" && viewModel.filterMonth != "ALL"
    val carryOverFromPrevMonths = if (isMonthFiltered) {
        val selYear = viewModel.filterYear.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val selMonth = viewModel.filterMonth.toIntOrNull() ?: Calendar.getInstance().get(Calendar.MONTH)
        
        val selCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selYear)
            set(Calendar.MONTH, selMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfSelectedMonth = selCal.timeInMillis
        
        val prevTxs = generalTransactions.filter { it.dateTime < startOfSelectedMonth }
        val prevInc = prevTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val prevExp = prevTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        
        val sal = if (profile.isSalaryIncluded) profile.monthlySalary else 0.0
        profile.openingBalance + sal + prevInc - prevExp
    } else {
        0.0
    }

    // 2. Process all transactions chronologically to calculate running balance and daily summary
    val allTransactionsSorted = generalTransactions.sortedBy { it.dateTime }
    val dailyStatsMap = LinkedHashMap<String, DailySummaryModel>()
    
    // Running balance starts with opening balance (and monthly salary if included)
    var runningBalance = profile.openingBalance + (if (profile.isSalaryIncluded) profile.monthlySalary else 0.0)
    
    val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFormat = SimpleDateFormat("d MMMM yy", Locale.US)
    
    allTransactionsSorted.forEach { tx ->
        val key = keyFormat.format(Date(tx.dateTime))
        
        if (tx.type == "INCOME") {
            runningBalance += tx.amount
        } else {
            runningBalance -= tx.amount
        }
        
        val stats = dailyStatsMap.getOrPut(key) {
            DailySummaryModel(
                dateString = convertToBengaliNumber(labelFormat.format(Date(tx.dateTime))),
                timestamp = tx.dateTime,
                income = 0.0,
                expense = 0.0,
                balanceAtEnd = 0.0
            )
        }
        
        if (tx.type == "INCOME") {
            stats.income += tx.amount
        } else {
            stats.expense += tx.amount
        }
        stats.balanceAtEnd = runningBalance
    }
    
    // Filter the daily summary days based on dropdown filters (Year, Month, Day) and sort descending (newest first)
    val activeDailySummaries = dailyStatsMap.values.toList().filter { summary ->
        val cal = Calendar.getInstance().apply { timeInMillis = summary.timestamp }
        val matchYear = viewModel.filterYear == "ALL" || cal.get(Calendar.YEAR).toString() == viewModel.filterYear
        val matchMonth = viewModel.filterMonth == "ALL" || cal.get(Calendar.MONTH).toString() == viewModel.filterMonth
        val matchDay = viewModel.filterDay == "ALL" || cal.get(Calendar.DAY_OF_MONTH).toString() == viewModel.filterDay
        
        matchYear && matchMonth && matchDay
    }.sortedByDescending { it.timestamp }

    // Category Lists
    val defaultIncomeCategories = listOf("Salary 💼", "Business 📈", "Freelancing 💻", "Investment 🏦", "Gift 🎁", "Others 🪙")
    val defaultExpenseCategories = listOf("Food 🍔", "Rent 🏠", "Bills ⚡", "Transport 🚗", "Shopping 🛍️", "Medicine 💊", "Entertainment 🎬", "Education 📚", "Others 💸")

    val activeCategories = if (viewModel.activeFormType == "INCOME") defaultIncomeCategories else defaultExpenseCategories

    // Apply Filters first so we can calculate and display totals at the absolute top
    val filteredTxList = generalTransactions.filter { tx ->
        val calTx = Calendar.getInstance().apply { timeInMillis = tx.dateTime }
        val matchYear = viewModel.filterYear == "ALL" || calTx.get(Calendar.YEAR).toString() == viewModel.filterYear
        val matchMonth = viewModel.filterMonth == "ALL" || calTx.get(Calendar.MONTH).toString() == viewModel.filterMonth
        val matchDay = viewModel.filterDay == "ALL" || calTx.get(Calendar.DAY_OF_MONTH).toString() == viewModel.filterDay
        val matchCategory = viewModel.filterCategoryQuery.trim().isEmpty() || tx.category.contains(viewModel.filterCategoryQuery, ignoreCase = true)
        val matchType = viewModel.filterType == "ALL" || tx.type == viewModel.filterType

        matchYear && matchMonth && matchDay && matchCategory && matchType
    }.sortedByDescending { it.dateTime }

    // Summary calculations filter ONLY by date, completely ignoring search filter
    val dateFilteredTxList = generalTransactions.filter { tx ->
        val calTx = Calendar.getInstance().apply { timeInMillis = tx.dateTime }
        val matchYear = viewModel.filterYear == "ALL" || calTx.get(Calendar.YEAR).toString() == viewModel.filterYear
        val matchMonth = viewModel.filterMonth == "ALL" || calTx.get(Calendar.MONTH).toString() == viewModel.filterMonth
        val matchDay = viewModel.filterDay == "ALL" || calTx.get(Calendar.DAY_OF_MONTH).toString() == viewModel.filterDay

        matchYear && matchMonth && matchDay
    }

    val filteredIncomeSum = dateFilteredTxList.filter { it.type == "INCOME" }.sumOf { it.amount }
    val filteredExpenseSum = dateFilteredTxList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val filteredBalance = filteredIncomeSum - filteredExpenseSum + carryOverFromPrevMonths

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 0. Dynamic visual summary card for the active filtered set (Budget Style)
        val effectiveTotalIncome = filteredIncomeSum + carryOverFromPrevMonths
        val expenseRatio = if (effectiveTotalIncome > 0) {
            (filteredExpenseSum / effectiveTotalIncome).toFloat().coerceIn(0f, 1f)
        } else if (filteredExpenseSum > 0) {
            1f
        } else {
            0f
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0060A8)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isMonthFiltered) "Monthly Balance" else "Current Balance",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "৳ " + convertToBengaliNumber(String.format(Locale.US, "%,.0f", filteredBalance)),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (viewModel.filterType == "INCOME") Color(0xFF16A34A) else Color.White.copy(alpha = 0.22f))
                            .then(
                                if (viewModel.filterType == "INCOME") Modifier.border(1.5.dp, Color.White, RoundedCornerShape(10.dp)) else Modifier
                            )
                            .clickable {
                                viewModel.filterType = if (viewModel.filterType == "INCOME") "ALL" else "INCOME"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💸", fontSize = 18.sp)
                    }
                }

                // Middle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Income",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "৳ " + convertToBengaliNumber(String.format(Locale.US, "%,.0f", filteredIncomeSum)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF86EFAC)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Expense",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "৳ " + convertToBengaliNumber(String.format(Locale.US, "%,.0f", filteredExpenseSum)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }

                // Bottom Progress Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isMonthFiltered) "Prev. Month:" else "Expense Ratio",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            if (isMonthFiltered) {
                                Text(
                                    text = "৳ " + convertToBengaliNumber(String.format(Locale.US, "%,.0f", carryOverFromPrevMonths)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF86EFAC)
                                )
                            }
                        }
                        Text(
                            text = "${(expenseRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { expenseRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFCA5A5),
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
            }
        }

        if (viewModel.showAddTransactionDialog) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.showAddTransactionDialog = false
                    viewModel.cancelEditing()
                },
                title = {
                    val titleText = if (viewModel.editingTransactionId != null) {
                        "Edit Transaction ✏️"
                    } else if (dialogType == "INCOME") {
                        "Add Income 📈"
                    } else {
                        "Add Expense 📉"
                    }
                    Text(
                        text = titleText,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 0. Toggle between INCOME and EXPENSE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    dialogType = "INCOME"
                                    viewModel.activeFormType = "INCOME"
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (dialogType == "INCOME") Color(0xFF2E7D32) else Color(0xFFF1F5F9),
                                    contentColor = if (dialogType == "INCOME") Color.White else Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Income 📈", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    dialogType = "EXPENSE"
                                    viewModel.activeFormType = "EXPENSE"
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (dialogType == "EXPENSE") Color(0xFFC62828) else Color(0xFFF1F5F9),
                                    contentColor = if (dialogType == "EXPENSE") Color.White else Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Expense 📉", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        val calendar = Calendar.getInstance()
                        if (viewModel.dateTimeInput.isNotEmpty()) {
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                                val parsedDate = sdf.parse(viewModel.dateTimeInput)
                                if (parsedDate != null) {
                                    calendar.time = parsedDate
                                }
                            } catch (e: Exception) {}
                        } else {
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                            viewModel.dateTimeInput = sdf.format(calendar.time)
                        }

                        val datePickerDialog = android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        calendar.set(Calendar.MINUTE, minute)
                                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                                        viewModel.dateTimeInput = sdf.format(calendar.time)
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )

                        // 1 & 2. Category & Amount side-by-side
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = viewModel.categoryInput,
                                onValueChange = { viewModel.categoryInput = it },
                                placeholder = { Text("Category (e.g. food)", fontSize = 14.sp, maxLines = 1, softWrap = false) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedBorderColor = if (dialogType == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                modifier = Modifier.weight(1.2f).defaultMinSize(minHeight = 52.dp)
                            )

                            OutlinedTextField(
                                value = viewModel.amountInput,
                                onValueChange = { viewModel.amountInput = it },
                                placeholder = { Text("Amount (৳)", fontSize = 14.sp, maxLines = 1, softWrap = false) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedBorderColor = if (dialogType == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp)
                            )
                        }

                        // Category Suggestions
                        if (filteredCategorySuggestions.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(filteredCategorySuggestions) { suggestion ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .border(BorderStroke(0.5.dp, Color(0xFFE2E8F0)), RoundedCornerShape(6.dp))
                                            .clickable { viewModel.categoryInput = suggestion }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = suggestion, fontSize = 11.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // 3. Date & Time picker
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() }
                        ) {
                            OutlinedTextField(
                                value = viewModel.dateTimeInput,
                                onValueChange = { },
                                readOnly = true,
                                enabled = false,
                                placeholder = { Text("Date & Time", fontSize = 14.sp, maxLines = 1, softWrap = false) },
                                trailingIcon = {
                                    IconButton(onClick = { datePickerDialog.show() }) {
                                        Text("🕒", fontSize = 13.sp)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color(0xFF0F172A),
                                    disabledBorderColor = if (dialogType == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828),
                                    disabledPlaceholderColor = Color(0xFF64748B)
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp)
                            )
                        }

                        // Group/Person input field
                        OutlinedTextField(
                            value = viewModel.selectedPersonName,
                            onValueChange = { viewModel.selectedPersonName = it },
                            placeholder = { Text("গ্রুপ/ব্যক্তি (যেমন: সাধারণ, বন্ধু...)", fontSize = 14.sp, maxLines = 1, softWrap = false) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = if (dialogType == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp)
                        )

                        // Group/Person Suggestions
                        if (filteredGroupSuggestions.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(filteredGroupSuggestions) { suggestion ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .border(BorderStroke(0.5.dp, Color(0xFFE2E8F0)), RoundedCornerShape(6.dp))
                                            .clickable { viewModel.selectedPersonName = suggestion }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = suggestion, fontSize = 11.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // 4. Description (Details)
                        OutlinedTextField(
                            value = viewModel.noteInput,
                            onValueChange = { viewModel.noteInput = it },
                            placeholder = { Text("Enter details (optional)...", fontSize = 14.sp) },
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = if (dialogType == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 72.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = viewModel.amountInput.toDoubleOrNull()
                            if (amount == null || amount <= 0.0) {
                                Toast.makeText(context, "Please enter a valid amount!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.saveTransaction()
                                viewModel.showAddTransactionDialog = false
                                Toast.makeText(context, "Record saved successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (dialogType == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828)
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        val btnText = if (viewModel.editingTransactionId != null) "Update" else "Save"
                        Text(text = btnText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.showAddTransactionDialog = false
                            viewModel.cancelEditing()
                        }
                    ) {
                        Text("Cancel", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // 2. Filter System Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🔍 Filter & Search Records",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                var isSearchVisible by remember { mutableStateOf(false) }
                var searchViewMode by remember { mutableStateOf("CATEGORY") } // "CATEGORY" or "GROUP"
                var yearMenuExpanded by remember { mutableStateOf(false) }
                var monthMenuExpanded by remember { mutableStateOf(false) }
                var dayMenuExpanded by remember { mutableStateOf(false) }

                val monthNames = listOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )

                val selectedYearText = viewModel.filterYear

                val availableMonths = if (viewModel.filterYear == "ALL") {
                    emptyList()
                } else {
                    (0..11).toList()
                }

                val selectedMonthText = if (viewModel.filterMonth == "ALL") {
                    "All Months"
                } else {
                    monthNames.getOrNull(viewModel.filterMonth.toIntOrNull() ?: -1) ?: "All Months"
                }

                val availableDays = if (viewModel.filterYear == "ALL" || viewModel.filterMonth == "ALL") {
                    emptyList()
                } else {
                    val yearVal = viewModel.filterYear.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                    val monthVal = viewModel.filterMonth.toIntOrNull() ?: Calendar.getInstance().get(Calendar.MONTH)
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, yearVal)
                        set(Calendar.MONTH, monthVal)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    (1..maxDays).toList()
                }

                val selectedDayText = if (viewModel.filterDay == "ALL") {
                    "All Days"
                } else {
                    "Day ${viewModel.filterDay}"
                }

                // Row of cascading filters: Year, Month, Day, Search button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Year Dropdown
                    Box(modifier = Modifier.weight(1.1f)) {
                        Button(
                            onClick = { yearMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(34.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (viewModel.filterYear == "ALL") "All Years" else "$selectedYearText",
                                    color = Color(0xFF0F172A),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("▼", color = Color(0xFF64748B), fontSize = 6.sp)
                            }
                        }

                        DropdownMenu(
                            expanded = yearMenuExpanded,
                            onDismissRequest = { yearMenuExpanded = false },
                            modifier = Modifier
                                .background(Color.White)
                                .heightIn(max = 240.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Years", color = Color(0xFF0F172A), fontSize = 11.sp) },
                                onClick = {
                                    viewModel.filterYear = "ALL"
                                    viewModel.filterMonth = "ALL"
                                    viewModel.filterDay = "ALL"
                                    yearMenuExpanded = false
                                }
                            )
                            (2026..2099).forEach { y ->
                                DropdownMenuItem(
                                    text = { Text(y.toString(), color = Color(0xFF0F172A), fontSize = 11.sp) },
                                    onClick = {
                                        viewModel.filterYear = y.toString()
                                        viewModel.filterMonth = "ALL"
                                        viewModel.filterDay = "ALL"
                                        yearMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Month Dropdown
                    Box(modifier = Modifier.weight(1.1f)) {
                        Button(
                            onClick = {
                                if (viewModel.filterYear == "ALL") {
                                    Toast.makeText(context, "Please select year first!", Toast.LENGTH_SHORT).show()
                                } else {
                                    monthMenuExpanded = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(34.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.filterYear == "ALL") Color(0xFFF1F5F9).copy(alpha = 0.5f) else Color(0xFFF1F5F9)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (viewModel.filterMonth == "ALL") "All Months" else "$selectedMonthText",
                                    color = if (viewModel.filterYear == "ALL") Color(0xFF94A3B8) else Color(0xFF0F172A),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("▼", color = Color(0xFF64748B), fontSize = 6.sp)
                            }
                        }

                        DropdownMenu(
                            expanded = monthMenuExpanded,
                            onDismissRequest = { monthMenuExpanded = false },
                            modifier = Modifier
                                .background(Color.White)
                                .heightIn(max = 240.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Months", color = Color(0xFF0F172A), fontSize = 11.sp) },
                                onClick = {
                                    viewModel.filterMonth = "ALL"
                                    viewModel.filterDay = "ALL"
                                    monthMenuExpanded = false
                                }
                            )
                            availableMonths.forEach { mIdx ->
                                DropdownMenuItem(
                                    text = { Text(monthNames[mIdx], color = Color(0xFF0F172A), fontSize = 11.sp) },
                                    onClick = {
                                        viewModel.filterMonth = mIdx.toString()
                                        viewModel.filterDay = "ALL"
                                        monthMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. Day Dropdown
                    Box(modifier = Modifier.weight(1.1f)) {
                        Button(
                            onClick = {
                                if (viewModel.filterYear == "ALL" || viewModel.filterMonth == "ALL") {
                                    Toast.makeText(context, "Please select year and month first!", Toast.LENGTH_SHORT).show()
                                } else {
                                    dayMenuExpanded = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(34.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.filterYear == "ALL" || viewModel.filterMonth == "ALL") Color(0xFFF1F5F9).copy(alpha = 0.5f) else Color(0xFFF1F5F9)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (viewModel.filterDay == "ALL") "All Days" else "$selectedDayText",
                                    color = if (viewModel.filterYear == "ALL" || viewModel.filterMonth == "ALL") Color(0xFF94A3B8) else Color(0xFF0F172A),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("▼", color = Color(0xFF64748B), fontSize = 6.sp)
                            }
                        }

                        DropdownMenu(
                            expanded = dayMenuExpanded,
                            onDismissRequest = { dayMenuExpanded = false },
                            modifier = Modifier
                                .background(Color.White)
                                .heightIn(max = 240.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Days", color = Color(0xFF0F172A), fontSize = 11.sp) },
                                onClick = {
                                    viewModel.filterDay = "ALL"
                                    dayMenuExpanded = false
                                }
                            )
                            availableDays.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text("Day $d", color = Color(0xFF0F172A), fontSize = 11.sp) },
                                    onClick = {
                                        viewModel.filterDay = d.toString()
                                        dayMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 4. Toggle Search Button
                    Button(
                        onClick = { isSearchVisible = !isSearchVisible },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSearchVisible) Color(0xFF1976D2) else Color(0xFFF1F5F9)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        modifier = Modifier.weight(0.9f).height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("🔍", fontSize = 9.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (isSearchVisible) "Hide" else "Search",
                                color = if (isSearchVisible) Color.White else Color(0xFF0F172A),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Smooth animated visibility for category search field and summary list
                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.filterCategoryQuery,
                            onValueChange = { viewModel.filterCategoryQuery = it },
                            placeholder = { Text("e.g. food", fontSize = 14.sp) },
                            trailingIcon = { Text("🔍", modifier = Modifier.padding(end = 8.dp), fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        // Toggle view between Category and Group/Person summary list
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ভিউ টাইপ:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                            listOf(
                                "CATEGORY" to "📂 ক্যাটাগরি তালিকা",
                                "GROUP" to "👥 গ্রুপ তালিকা"
                            ).forEach { (mode, label) ->
                                val isSelected = searchViewMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF1976D2) else Color(0xFFE2E8F0))
                                        .clickable { searchViewMode = mode }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Calculate Expense Category Totals from dateFilteredTxList
                        val expenseCategoryTotals = remember(dateFilteredTxList, viewModel.filterCategoryQuery) {
                            dateFilteredTxList
                                .filter { it.type == "EXPENSE" }
                                .groupBy { it.category.trim() }
                                .mapValues { entry -> entry.value.sumOf { it.amount } }
                                .filter { (cat, _) ->
                                    viewModel.filterCategoryQuery.trim().isEmpty() || cat.contains(viewModel.filterCategoryQuery, ignoreCase = true)
                                }
                                .toList()
                                .sortedByDescending { it.second }
                        }

                        // Calculate Expense Group (Person/Group Name) Totals from dateFilteredTxList
                        val expenseGroupTotals = remember(dateFilteredTxList, viewModel.filterCategoryQuery) {
                            dateFilteredTxList
                                .filter { it.type == "EXPENSE" }
                                .groupBy { it.personName.trim() }
                                .mapValues { entry -> entry.value.sumOf { it.amount } }
                                .filter { (person, _) ->
                                    val displayPerson = if (person.isEmpty() || person.equals("General", ignoreCase = true) || person.equals("সাধারণ", ignoreCase = true)) "সাধারণ (General)" else person
                                    viewModel.filterCategoryQuery.trim().isEmpty() || displayPerson.contains(viewModel.filterCategoryQuery, ignoreCase = true)
                                }
                                .toList()
                                .sortedByDescending { it.second }
                        }

                        if (searchViewMode == "CATEGORY") {
                            // 1. Category Summary Card (Full Width)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "📂 ক্যাটাগরি খরচ:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )

                                    if (expenseCategoryTotals.isEmpty()) {
                                        Text(
                                            text = "কোনো ক্যাটাগরি পাওয়া যায়নি",
                                            fontSize = 9.sp,
                                            color = Color(0xFF64748B),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    } else {
                                        expenseCategoryTotals.forEach { (category, total) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White, shape = RoundedCornerShape(6.dp))
                                                    .border(BorderStroke(0.5.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.List,
                                                        contentDescription = null,
                                                        tint = Color(0xFF1976D2),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = if (category.isEmpty()) "অন্যান্য" else category,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF0F172A),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = "৳${String.format(Locale.US, "%,.0f", total)}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFC62828)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // 2. Group/Person Summary Card (Full Width)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "👥 গ্রুপ খরচ:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )

                                    if (expenseGroupTotals.isEmpty()) {
                                        Text(
                                            text = "কোনো গ্রুপ পাওয়া যায়নি",
                                            fontSize = 9.sp,
                                            color = Color(0xFF64748B),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    } else {
                                        expenseGroupTotals.forEach { (person, total) ->
                                            val displayName = if (person.isEmpty() || person.equals("General", ignoreCase = true) || person.equals("সাধারণ", ignoreCase = true)) "সাধারণ" else person
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White, shape = RoundedCornerShape(6.dp))
                                                    .border(BorderStroke(0.5.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = Color(0xFF1976D2),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = displayName,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF0F172A),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = "৳${String.format(Locale.US, "%,.0f", total)}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFC62828)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Transactions Record Card List
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (viewModel.filterType == "INCOME") "📈 Records List (শুধু আয় / Income)" else "📋 Records List",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        if (filteredTxList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("💸", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No records found!",
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Add new income or expense entries or change your filters.",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        } else {
            val itemsToShow = if (viewModel.showAllTransactions) filteredTxList else filteredTxList.take(5)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Default Flat List Rendering
                            itemsToShow.forEach { tx ->
                                TransactionItemRow(
                                    transaction = tx,
                                    onEdit = {
                                        if (tx.isPersonal) {
                                            Toast.makeText(context, "ব্যক্তিগত লেনদেনটি এডিট করতে দয়া করে অ্যাকাউন্ট (Account) ট্যাবে যান।", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.startEditingTransaction(tx)
                                        }
                                    },
                                    onDelete = {
                                        if (tx.isPersonal) {
                                            Toast.makeText(context, "ব্যক্তিগত লেনদেনটি ডিলিট করতে দয়া করে অ্যাকাউন্ট (Account) ট্যাবে যান।", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.confirmDelete(
                                                title = "Delete Transaction",
                                                message = "Are you sure you want to delete this transaction?",
                                                action = {
                                                    viewModel.deleteTransactionWithUndo(tx, showSnackbarWithUndo)
                                                }
                                            )
                                        }
                                    },
                                    onDoubleClick = { viewModel.selectedTransactionForDetails = tx }
                                )
                            }
                        }

                        // Bottom fade overlay to indicate there are more items tucked downwards
                        if (!viewModel.showAllTransactions && filteredTxList.size > 5) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .height(40.dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.95f))
                                        )
                                    )
                            )
                        }
                    }

                    if (filteredTxList.size > 5) {
                        Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.showAllTransactions = !viewModel.showAllTransactions }
                                .background(Color(0xFFF8FAFC))
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (viewModel.showAllTransactions) "Show Less 🔼" else "Show All 🔽",
                                    color = Color(0xFF1976D2),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4. Daily Income & Expense Summary List Card
        var showAllDailySummaries by remember { mutableStateOf(false) }
        val dailySummariesToShow = if (showAllDailySummaries) activeDailySummaries else activeDailySummaries.take(7)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "📅 Daily Summary",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Date", fontSize = 10.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
                        Text(text = "Income", fontSize = 10.sp, color = Color(0xFF2E7D32), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                        Text(text = "Expense", fontSize = 10.sp, color = Color(0xFFC62828), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                        Text(text = "Balance", fontSize = 10.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                    }
                    
                    Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                    dailySummariesToShow.forEach { summary ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 0.5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = summary.dateString,
                                fontSize = 10.sp,
                                color = Color(0xFF334155),
                                modifier = Modifier.weight(1.2f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (summary.income > 0.0) "৳" + convertToBengaliNumber(String.format(Locale.US, "%,.0f", summary.income)) else "0",
                                fontSize = 10.sp,
                                color = if (summary.income > 0.0) Color(0xFF2E7D32) else Color(0xFF94A3B8),
                                modifier = Modifier.weight(1.0f),
                                textAlign = TextAlign.End,
                                fontWeight = if (summary.income > 0.0) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = if (summary.expense > 0.0) "৳" + convertToBengaliNumber(String.format(Locale.US, "%,.0f", summary.expense)) else "0",
                                fontSize = 10.sp,
                                color = if (summary.expense > 0.0) Color(0xFFC62828) else Color(0xFF94A3B8),
                                modifier = Modifier.weight(1.0f),
                                textAlign = TextAlign.End,
                                fontWeight = if (summary.expense > 0.0) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "৳" + convertToBengaliNumber(String.format(Locale.US, "%,.0f", summary.balanceAtEnd)),
                                fontSize = 10.sp,
                                color = if (summary.balanceAtEnd >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.weight(1.2f),
                                textAlign = TextAlign.End,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Divider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                    }

                    if (activeDailySummaries.size > 7) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { showAllDailySummaries = !showAllDailySummaries },
                            modifier = Modifier.fillMaxWidth().height(34.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (showAllDailySummaries) "Show Less 🔼" else "Show All 🔽",
                                color = Color(0xFF0F172A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDoubleClick: () -> Unit
) {
    val dateString = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(transaction.dateTime))
    val isIncome = transaction.type == "INCOME"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onDoubleClick,
                onDoubleClick = onDoubleClick,
                onLongClick = onDelete
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = 1.dp,
            color = if (isIncome) Color(0xFF2E7D32).copy(alpha = 0.12f) else Color(0xFFC62828).copy(alpha = 0.12f)
        )
    ) {
        val startColor = if (isIncome) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFC62828).copy(alpha = 0.15f)
        val endColor = Color.Transparent

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(
                    brush = Brush.horizontalGradient(
                        0.0f to startColor,
                        0.8f to startColor.copy(alpha = 0.03f),
                        1.0f to endColor
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color bar, flush to the left border
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828))
            )

            // Content padding starts here
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left block: category name & details
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Column {
                        val hasGroup = transaction.personName.isNotEmpty() &&
                                !transaction.personName.trim().equals("সাধারণ", ignoreCase = true) &&
                                !transaction.personName.trim().equals("general", ignoreCase = true)

                        val dispCategory = if (transaction.isPersonal) transaction.personName else transaction.category

                        Text(
                            text = dispCategory,
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = convertToBengaliNumber(dateString),
                                color = Color(0xFF64748B),
                                fontSize = 9.sp
                            )
                            if (transaction.isPersonal) {
                                // সাব টাইটেল e কিছু প্রদর্শিত হবে না
                            } else if (hasGroup) {
                                Text(
                                    text = "•",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = "👥 ${transaction.personName}",
                                    color = Color(0xFF1976D2),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Right block
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "৳" + convertToBengaliNumber(String.format(Locale.US, "%,.0f", transaction.amount)),
                        color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1976D2).copy(alpha = 0.08f))
                            .clickable { onEdit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✏️", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ============================================================================
// SESSION 3: NOTEBOOK (NOTICE) SESSION VIEW
// ============================================================================

data class NoteChecklistItem(
    val isChecked: Boolean = false,
    val text: String = ""
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoticeSessionView(
    viewModel: FinanceViewModel,
    notices: List<NoticeEntity>,
    showSnackbarWithUndo: (String, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var noticeContentInputState by remember(viewModel.showAddNoticeDialog, viewModel.editingNotice) {
        mutableStateOf(TextFieldValue(viewModel.noticeContentInput))
    }

    var isChecklistMode by remember(viewModel.showAddNoticeDialog, viewModel.editingNotice) {
        val raw = viewModel.noticeContentInput
        mutableStateOf(raw.contains("[ ]") || raw.contains("[x]") || raw.contains("[X]"))
    }

    val checklistItems = remember(viewModel.showAddNoticeDialog, viewModel.editingNotice) {
        val list = mutableStateListOf<NoteChecklistItem>()
        val lines = viewModel.noticeContentInput.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[ ]")) {
                list.add(NoteChecklistItem(false, line.substringAfter("[ ]")))
            } else if (trimmed.startsWith("[x]") || trimmed.startsWith("[X]")) {
                list.add(NoteChecklistItem(true, line.substringAfter("[x]").substringAfter("[X]")))
            } else if (line.isNotEmpty()) {
                list.add(NoteChecklistItem(false, line))
            }
        }
        if (list.isEmpty()) {
            list.add(NoteChecklistItem(false, ""))
        }
        list
    }

    // Note Creator Dialog
    if (viewModel.showAddNoticeDialog) {
        Dialog(
            onDismissRequest = { 
                viewModel.showAddNoticeDialog = false 
                viewModel.editingNotice = null
                viewModel.noticeTitleInput = ""
                viewModel.noticeContentInput = ""
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 580.dp)
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dialogTitle = if (viewModel.editingNotice != null) "📓 নোট এডিট করুন" else "📓 নতুন নোট ও চেকলিস্ট"
                        Text(
                            text = dialogTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        IconButton(
                            onClick = { 
                                viewModel.showAddNoticeDialog = false 
                                viewModel.editingNotice = null
                                viewModel.noticeTitleInput = ""
                                viewModel.noticeContentInput = ""
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("❌", fontSize = 12.sp)
                        }
                    }

                    // Mode Selection Tabs (Text vs Checklist)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isChecklistMode) Color.White else Color.Transparent)
                                .clickable {
                                    if (isChecklistMode) {
                                        val combined = checklistItems.joinToString("\n") { it.text }
                                        noticeContentInputState = TextFieldValue(combined)
                                        viewModel.noticeContentInput = combined
                                        isChecklistMode = false
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Text",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isChecklistMode) Color(0xFF1976D2) else Color(0xFF64748B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChecklistMode) Color.White else Color.Transparent)
                                .clickable {
                                    if (!isChecklistMode) {
                                        val lines = noticeContentInputState.text.lines()
                                        checklistItems.clear()
                                        lines.forEach { line ->
                                            val trimmed = line.trim()
                                            if (trimmed.startsWith("[ ]")) {
                                                checklistItems.add(NoteChecklistItem(false, line.substringAfter("[ ]")))
                                            } else if (trimmed.startsWith("[x]") || trimmed.startsWith("[X]")) {
                                                checklistItems.add(NoteChecklistItem(true, line.substringAfter("[x]").substringAfter("[X]")))
                                            } else {
                                                checklistItems.add(NoteChecklistItem(false, line))
                                            }
                                        }
                                        if (checklistItems.isEmpty()) {
                                            checklistItems.add(NoteChecklistItem(false, ""))
                                        }
                                        isChecklistMode = true
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Checklist",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isChecklistMode) Color(0xFF1976D2) else Color(0xFF64748B)
                            )
                        }
                    }

                    // Title / Header input
                    OutlinedTextField(
                        value = viewModel.noticeTitleInput,
                        onValueChange = { viewModel.noticeTitleInput = it },
                        placeholder = { Text("নোট শিরোনাম (Title)", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )

                    // Content Section
                    if (!isChecklistMode) {
                        OutlinedTextField(
                            value = noticeContentInputState,
                            onValueChange = { 
                                noticeContentInputState = it 
                                viewModel.noticeContentInput = it.text
                            },
                            placeholder = { Text("এখানে নোট বিস্তারিত লিখুন...", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                    } else {
                        // Interactive Checklist Editor
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "চেকলিস্ট আইটেমসমূহ:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )

                            checklistItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { checked ->
                                            checklistItems[index] = item.copy(isChecked = checked)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1976D2)),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedTextField(
                                        value = item.text,
                                        onValueChange = { txt ->
                                            checklistItems[index] = item.copy(text = txt)
                                        },
                                        placeholder = { Text("আইটেম নাম...", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 13.sp,
                                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                            color = if (item.isChecked) Color(0xFF94A3B8) else Color(0xFF0F172A)
                                        ),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = {
                                            checklistItems.add(index + 1, NoteChecklistItem(false, ""))
                                        }),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF1976D2),
                                            unfocusedBorderColor = Color(0xFFE2E8F0)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            if (checklistItems.size > 1) {
                                                checklistItems.removeAt(index)
                                            } else {
                                                checklistItems[0] = NoteChecklistItem(false, "")
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "আইটেম মুছুন",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Add Item Button
                            OutlinedButton(
                                onClick = {
                                    checklistItems.add(NoteChecklistItem(false, ""))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFEFF6FF))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "নতুন চেকলিস্ট আইটেম যোগ করুন",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                            }
                        }
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            if (isChecklistMode) {
                                val validItems = checklistItems.filter { it.text.isNotBlank() }
                                val serialized = if (validItems.isNotEmpty()) {
                                    validItems.joinToString("\n") { (if (it.isChecked) "[x] " else "[ ] ") + it.text }
                                } else {
                                    ""
                                }
                                viewModel.noticeContentInput = serialized
                            } else {
                                viewModel.noticeContentInput = noticeContentInputState.text
                            }

                            if (viewModel.noticeContentInput.trim().isEmpty() && viewModel.noticeTitleInput.trim().isEmpty()) {
                                Toast.makeText(context, "দয়া করে শিরোনাম অথবা বিস্তারিত লিখুন!", Toast.LENGTH_SHORT).show()
                            } else {
                                val msg = if (viewModel.editingNotice != null) "নোট আপডেট হয়েছে! 📓" else "নোট সেভ হয়েছে! 📓"
                                viewModel.saveNotice()
                                viewModel.showAddNoticeDialog = false
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        val buttonText = if (viewModel.editingNotice != null) "নোট আপডেট করুন 📓" else "নোট সেভ করুন 📓"
                        Text(buttonText, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    var selectedNoticeForDetail by remember { mutableStateOf<NoticeEntity?>(null) }
    var isReorderMode by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isReorderMode) "সাজানোর মুড (উপরে/নিচে নামান)" else "📋 Saved Notes (${convertToBengaliNumber(notices.size.toString())})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isReorderMode) Color(0xFFD97706) else Color(0xFF0F172A),
                modifier = Modifier.weight(1f)
            )

            if (notices.isNotEmpty()) {
                TextButton(
                    onClick = { isReorderMode = !isReorderMode },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = if (isReorderMode) Icons.Default.Close else Icons.Default.SwapVert,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isReorderMode) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isReorderMode) "বাতিল" else "সাজান",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isReorderMode) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (notices.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📓", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your notebook is empty!",
                        color = Color(0xFF475569),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Save your notes, shopping lists, or targets here. Tap 'নতুন নোট' to start.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                notices.forEachIndexed { index, notice ->
                    StickyNoteCard(
                        notice = notice,
                        onOpenDetail = {
                            selectedNoticeForDetail = notice
                        },
                        onDelete = {
                            viewModel.confirmDelete(
                                title = "Delete Note",
                                message = "Are you sure you want to delete this note?",
                                action = {
                                    viewModel.deleteNoticeWithUndo(notice, showSnackbarWithUndo)
                                }
                            )
                        },
                        onEdit = {
                            viewModel.editingNotice = notice
                            val parsedTitle = if (notice.content.contains("===NOTE_TITLE===")) {
                                notice.content.substringBefore("===NOTE_TITLE===")
                            } else {
                                ""
                            }
                            val parsedContent = if (notice.content.contains("===NOTE_TITLE===")) {
                                notice.content.substringAfter("===NOTE_TITLE===")
                            } else {
                                notice.content
                            }
                            viewModel.noticeTitleInput = parsedTitle
                            viewModel.noticeContentInput = parsedContent
                            viewModel.showAddNoticeDialog = true
                        },
                        isReorderMode = isReorderMode,
                        canMoveUp = index > 0,
                        canMoveDown = index < notices.size - 1,
                        onMoveUp = { viewModel.moveNoticeUp(notice, notices) },
                        onMoveDown = { viewModel.moveNoticeDown(notice, notices) }
                    )
                }
            }
        }
    }

    // Full Screen Note Detail Dialog
    if (selectedNoticeForDetail != null) {
        val currentNotice = notices.find { it.id == selectedNoticeForDetail?.id } ?: selectedNoticeForDetail!!
        NoteDetailDialog(
            notice = currentNotice,
            onDismiss = { selectedNoticeForDetail = null },
            onEdit = {
                viewModel.editingNotice = currentNotice
                val parsedTitle = if (currentNotice.content.contains("===NOTE_TITLE===")) {
                    currentNotice.content.substringBefore("===NOTE_TITLE===")
                } else ""
                val parsedContent = if (currentNotice.content.contains("===NOTE_TITLE===")) {
                    currentNotice.content.substringAfter("===NOTE_TITLE===")
                } else currentNotice.content
                viewModel.noticeTitleInput = parsedTitle
                viewModel.noticeContentInput = parsedContent
                viewModel.showAddNoticeDialog = true
                selectedNoticeForDetail = null
            },
            onDelete = {
                val toDelete = currentNotice
                selectedNoticeForDetail = null
                viewModel.confirmDelete(
                    title = "Delete Note",
                    message = "Are you sure you want to delete this note?",
                    action = {
                        viewModel.deleteNoticeWithUndo(toDelete, showSnackbarWithUndo)
                    }
                )
            },
            onUpdate = { updated ->
                viewModel.updateNotice(updated)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StickyNoteCard(
    notice: NoticeEntity,
    onOpenDetail: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    isReorderMode: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {}
) {
    val dateString = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(notice.timestamp))

    val parsedTitle = if (notice.content.contains("===NOTE_TITLE===")) {
        notice.content.substringBefore("===NOTE_TITLE===")
    } else {
        ""
    }

    val parsedContent = if (notice.content.contains("===NOTE_TITLE===")) {
        notice.content.substringAfter("===NOTE_TITLE===")
    } else {
        notice.content
    }

    val lines = parsedContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val checklistCount = lines.count { it.startsWith("[ ]") || it.startsWith("[x]") || it.startsWith("[X]") }
    val completedCount = lines.count { it.startsWith("[x]") || it.startsWith("[X]") }
    val isChecklist = checklistCount > 0

    val displayTitle = parsedTitle.ifBlank {
        val clean = parsedContent.replace(Regex("\\[[ xX]\\]"), "").trim()
        if (clean.isNotEmpty()) {
            if (clean.length > 28) clean.take(28) + "..." else clean
        } else {
            "Untitled Note 📝"
        }
    }

    val previewText = if (isChecklist) {
        "☑️ $completedCount of $checklistCount items completed"
    } else {
        val clean = parsedContent.trim()
        if (clean.length > 50) clean.take(50) + "..." else clean
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetail() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isChecklist) Color(0xFFEFF6FF) else Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isChecklist) "☑️" else "📝", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (previewText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isChecklist) Color(0xFF1D4ED8) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📅 " + convertToBengaliNumber(dateString),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isReorderMode) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (canMoveUp) Color(0xFF0060A8) else Color(0xFFE2E8F0))
                            .clickable(enabled = canMoveUp) { onMoveUp() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = if (canMoveUp) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (canMoveDown) Color(0xFF0060A8) else Color(0xFFE2E8F0))
                            .clickable(enabled = canMoveDown) { onMoveDown() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = if (canMoveDown) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Note",
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Note",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailDialog(
    notice: NoticeEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (NoticeEntity) -> Unit
) {
    val dateString = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(notice.timestamp))

    val parsedTitle = if (notice.content.contains("===NOTE_TITLE===")) {
        notice.content.substringBefore("===NOTE_TITLE===")
    } else ""

    val parsedContent = if (notice.content.contains("===NOTE_TITLE===")) {
        notice.content.substringAfter("===NOTE_TITLE===")
    } else notice.content

    val displayTitle = parsedTitle.ifBlank { "Untitled Note 📝" }

    val lines = parsedContent.lines()
    val hasChecklist = lines.any { it.trim().startsWith("[ ]") || it.trim().startsWith("[x]") || it.trim().startsWith("[X]") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = displayTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Close"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color(0xFF1976D2)
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFDC2626)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF0F172A)
                    )
                )
            },
            containerColor = Color(0xFFF8FAFC)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📅", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = convertToBengaliNumber(dateString),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (hasChecklist) Color(0xFFEFF6FF) else Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = if (hasChecklist) "☑️ Checklist" else "📝 Text Note",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasChecklist) Color(0xFF1D4ED8) else Color(0xFFD97706),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        if (parsedTitle.isNotBlank()) {
                            Text(
                                text = parsedTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        SelectionContainer {
                            if (hasChecklist) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    lines.forEachIndexed { index, line ->
                                        val trimmed = line.trim()
                                        if (trimmed.startsWith("[ ]") || trimmed.startsWith("[x]") || trimmed.startsWith("[X]")) {
                                            val isChecked = trimmed.startsWith("[x]") || trimmed.startsWith("[X]")
                                            val text = if (trimmed.startsWith("[ ]")) trimmed.substring(3).trim() else trimmed.substring(3).trim()
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        val newLines = lines.toMutableList()
                                                        newLines[index] = if (isChecked) "[ ] $text" else "[x] $text"
                                                        val newCombined = if (parsedTitle.isNotEmpty()) "$parsedTitle===NOTE_TITLE===${newLines.joinToString("\n")}" else newLines.joinToString("\n")
                                                        onUpdate(notice.copy(content = newCombined))
                                                    }
                                                    .background(if (isChecked) Color(0xFFF8FAFC) else Color.Transparent)
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        val newLines = lines.toMutableList()
                                                        newLines[index] = if (checked) "[x] $text" else "[ ] $text"
                                                        val newCombined = if (parsedTitle.isNotEmpty()) "$parsedTitle===NOTE_TITLE===${newLines.joinToString("\n")}" else newLines.joinToString("\n")
                                                        onUpdate(notice.copy(content = newCombined))
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1976D2)),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = text,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isChecked) Color(0xFF94A3B8) else Color(0xFF1E293B),
                                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                                                )
                                            }
                                        } else if (line.isNotEmpty()) {
                                            Text(
                                                text = line,
                                                fontSize = 15.sp,
                                                color = Color(0xFF334155),
                                                lineHeight = 22.sp,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = parsedContent,
                                    fontSize = 15.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoubleTapDetailsDialog(transaction: TransactionEntity, onDismiss: () -> Unit) {
    val formattedDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.US).format(Date(transaction.dateTime))
    val isIncome = transaction.type == "INCOME"
    val isPersonal = transaction.isPersonal

    val displayDetails = if (isPersonal) {
        if (transaction.note.isNotBlank() && !transaction.note.startsWith("From/To ")) {
            transaction.note
        } else {
            transaction.category
        }
    } else {
        if (transaction.note.isNotBlank()) transaction.note else "No details provided."
    }

    val displayCategory = if (isPersonal) "Account" else transaction.category

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(
                width = 1.dp,
                color = if (isIncome) Color(0xFF2E7D32).copy(alpha = 0.5f) else Color(0xFFC62828).copy(alpha = 0.5f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Close button at top right
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                ) {
                    Text("❌", fontSize = 11.sp)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Title
                    Text(
                        text = "Transaction Details 🔎",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    // Main Content: Details (Note)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Details / বিবরণ:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = displayDetails,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            lineHeight = 18.sp
                        )
                    }

                    // Bottom info: Category on left, Money & Date small in the right corner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Category (on the bottom left)
                        Column {
                            Text(
                                text = "Category",
                                fontSize = 8.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = displayCategory,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            if (isPersonal) {
                                Text(
                                    text = "👤 Person: ${transaction.personName}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFC62828)
                                )
                            } else if (transaction.personName.isNotEmpty() &&
                                !transaction.personName.trim().equals("সাধারণ", ignoreCase = true) &&
                                !transaction.personName.trim().equals("general", ignoreCase = true)) {
                                Text(
                                    text = "👥 Group: ${transaction.personName}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1976D2)
                                )
                            }
                        }

                        // Money & Date in a corner (bottom right, small)
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Amount: " + (if (isIncome) "+" else "-") + "৳" + convertToBengaliNumber(String.format(Locale.US, "%,.0f", transaction.amount)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = convertToBengaliNumber(formattedDate),
                                fontSize = 9.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// SYSTEM NAVIGATION BAR
// ============================================================================

@Composable
fun BottomNavigationBar(activeTab: String, onTabSelected: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color(0xFF2563EB),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .fillMaxWidth()
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
        NavigationBarItem(
            selected = activeTab == "NOTICE",
            onClick = { onTabSelected("NOTICE") },
            icon = { Text("📓", fontSize = 18.sp) },
            label = { Text("Notebook", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) },
            modifier = Modifier
                .padding(horizontal = 2.dp, vertical = 2.dp)
                .background(
                    color = if (activeTab == "NOTICE") Color.White.copy(alpha = 0.15f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                selectedTextColor = Color.White,
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            selected = activeTab == "ACCOUNT",
            onClick = { onTabSelected("ACCOUNT") },
            icon = { Text("👥", fontSize = 18.sp) },
            label = { Text("Account", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) },
            modifier = Modifier
                .padding(horizontal = 2.dp, vertical = 2.dp)
                .background(
                    color = if (activeTab == "ACCOUNT") Color.White.copy(alpha = 0.15f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                selectedTextColor = Color.White,
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            selected = activeTab == "TRACKER",
            onClick = { onTabSelected("TRACKER") },
            icon = { Text("💸", fontSize = 18.sp) },
            label = { Text("Tracker", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) },
            modifier = Modifier
                .padding(horizontal = 2.dp, vertical = 2.dp)
                .background(
                    color = if (activeTab == "TRACKER") Color.White.copy(alpha = 0.15f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                selectedTextColor = Color.White,
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            selected = activeTab == "BAZAR",
            onClick = { onTabSelected("BAZAR") },
            icon = { Text("🛒", fontSize = 18.sp) },
            label = { Text("Bazar", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) },
            modifier = Modifier
                .padding(horizontal = 2.dp, vertical = 2.dp)
                .background(
                    color = if (activeTab == "BAZAR") Color.White.copy(alpha = 0.15f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                selectedTextColor = Color.White,
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            selected = activeTab == "BUDGET",
            onClick = { onTabSelected("BUDGET") },
            icon = { Text("📊", fontSize = 18.sp) },
            label = { Text("Budget", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) },
            modifier = Modifier
                .padding(horizontal = 2.dp, vertical = 2.dp)
                .background(
                    color = if (activeTab == "BUDGET") Color.White.copy(alpha = 0.15f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                selectedTextColor = Color.White,
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = Color.Transparent
            )
        )
        }
    }
}
