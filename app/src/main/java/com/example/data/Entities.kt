package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val dateString: String,
    val checklistJson: String = ""
) {
    fun getChecklistItems(): List<NoteChecklistItem> {
        return NoteChecklistSerializer.deserialize(checklistJson)
    }
}

@Entity(tableName = "people_dues")
data class PeopleDueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val initial: String,
    val amountOwed: Double = 0.0, // Amount user owes this person (Dena / -)
    val amountReceivable: Double = 0.0 // Amount this person owes user (Powna / +)
)

@Entity(tableName = "person_transactions")
data class PersonTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val details: String,
    val amount: Double,
    val dateTime: String,
    val isGive: Boolean = true // true = I give (আমি দিলাম / পাওনা +), false = I receive (আমি নিলাম / দেনা -)
)

@Entity(tableName = "transaction_payments")
data class TransactionPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val paidAmount: Double,
    val dateTime: String
)

@Entity(tableName = "tracker_records")
data class TrackerRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val isExpense: Boolean = true,
    val dateString: String,
    val category: String = "বাজার",
    val description: String = "",
    val year: Int = 2026,
    val month: String = "August",
    val day: String = "1"
)

@Entity(tableName = "bazar_items")
data class BazarItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetPrice: Double,
    val unitQuantity: String,
    val actualSpent: Double = 0.0
)

@Entity(tableName = "budget_expenses")
data class BudgetExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val tag: String,
    val dateText: String,
    val amount: Double
)
