package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}

@Dao
interface PeopleDueDao {
    @Query("SELECT * FROM people_dues ORDER BY id ASC")
    fun getAllDues(): Flow<List<PeopleDueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDue(due: PeopleDueEntity): Long

    @Update
    suspend fun updateDue(due: PeopleDueEntity)

    @Delete
    suspend fun deleteDue(due: PeopleDueEntity)
}

@Dao
interface PersonTransactionDao {
    @Query("SELECT * FROM person_transactions ORDER BY id DESC")
    fun getAllTransactions(): Flow<List<PersonTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: PersonTransactionEntity): Long

    @Delete
    suspend fun deleteTransaction(transaction: PersonTransactionEntity)

    @Query("SELECT * FROM transaction_payments ORDER BY id ASC")
    fun getAllPayments(): Flow<List<TransactionPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: TransactionPaymentEntity): Long
}

@Dao
interface TrackerDao {
    @Query("SELECT * FROM tracker_records ORDER BY id DESC")
    fun getAllRecords(): Flow<List<TrackerRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TrackerRecordEntity): Long

    @Update
    suspend fun updateRecord(record: TrackerRecordEntity)

    @Delete
    suspend fun deleteRecord(record: TrackerRecordEntity)
}

@Dao
interface BazarDao {
    @Query("SELECT * FROM bazar_items ORDER BY id ASC")
    fun getAllBazarItems(): Flow<List<BazarItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBazarItem(item: BazarItemEntity): Long

    @Update
    suspend fun updateBazarItem(item: BazarItemEntity)

    @Delete
    suspend fun deleteBazarItem(item: BazarItemEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget_expenses ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<BudgetExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: BudgetExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: BudgetExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: BudgetExpenseEntity)
}
