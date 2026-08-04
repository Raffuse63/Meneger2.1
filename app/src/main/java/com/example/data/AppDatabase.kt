package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        NoteEntity::class,
        PeopleDueEntity::class,
        TrackerRecordEntity::class,
        BazarItemEntity::class,
        BudgetExpenseEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun peopleDueDao(): PeopleDueDao
    abstract fun trackerDao(): TrackerDao
    abstract fun bazarDao(): BazarDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meneger_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            suspend fun populateInitialData(db: AppDatabase) {
                // Populate Notes
                val noteDao = db.noteDao()
                noteDao.insertNote(NoteEntity(title = "wifi", content = "somrat- 50...", dateString = "23 Jul 2026, 08:16 PM"))
                noteDao.insertNote(NoteEntity(title = ".github/workflows/bu...", content = "name: Build A...", dateString = "17 Jul 2026, 11:23 PM"))

                // Populate People Dues
                val dueDao = db.peopleDueDao()
                dueDao.insertDue(PeopleDueEntity(name = "রানা দোকানদার", initial = "র", amountOwed = 500.0, amountReceivable = 0.0))
                dueDao.insertDue(PeopleDueEntity(name = "আফা", initial = "আ", amountOwed = 2000.0, amountReceivable = 0.0))
                dueDao.insertDue(PeopleDueEntity(name = "বিকাশ", initial = "ব", amountOwed = 1000.0, amountReceivable = 0.0))
                dueDao.insertDue(PeopleDueEntity(name = "নাইমুর ভাই", initial = "ন", amountOwed = 2000.0, amountReceivable = 0.0))

                // Populate Tracker Records
                val trackerDao = db.trackerDao()
                trackerDao.insertRecord(TrackerRecordEntity(title = "মাথার তেল", amount = 700.0, isExpense = true, dateString = "01 Aug, 08:46 PM", category = "বাজার", year = 2026, month = "August", day = "1"))
                trackerDao.insertRecord(TrackerRecordEntity(title = "সেম্পু", amount = 30.0, isExpense = true, dateString = "01 Aug, 08:44 PM", category = "বাজার", year = 2026, month = "August", day = "1"))

                // Populate Bazar Items
                val bazarDao = db.bazarDao()
                bazarDao.insertBazarItem(BazarItemEntity(title = "মাটির মগ", targetPrice = 100.0, unitQuantity = "1 কেজি", actualSpent = 0.0))
                bazarDao.insertBazarItem(BazarItemEntity(title = "বেল্ট", targetPrice = 120.0, unitQuantity = "1 কেজি", actualSpent = 0.0))
                bazarDao.insertBazarItem(BazarItemEntity(title = "গাড়িভাড়া", targetPrice = 100.0, unitQuantity = "2 টি", actualSpent = 0.0))
                bazarDao.insertBazarItem(BazarItemEntity(title = "ঝাড়ু", targetPrice = 80.0, unitQuantity = "1 টি", actualSpent = 0.0))
                bazarDao.insertBazarItem(BazarItemEntity(title = "সরিষা তেল", targetPrice = 50.0, unitQuantity = "100 গ্রাম", actualSpent = 0.0))
                bazarDao.insertBazarItem(BazarItemEntity(title = "শসা", targetPrice = 60.0, unitQuantity = "1 কেজি", actualSpent = 0.0))
                bazarDao.insertBazarItem(BazarItemEntity(title = "লেবু", targetPrice = 30.0, unitQuantity = "1 কেজি", actualSpent = 0.0))

                // Populate Budget Expenses
                val budgetDao = db.budgetDao()
                budgetDao.insertExpense(BudgetExpenseEntity(title = "রুম ভাড়া", tag = "Payslip", dateText = "আজ", amount = 4500.0))
            }
        }
    }
}
