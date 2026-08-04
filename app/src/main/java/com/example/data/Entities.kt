package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val dateString: String
)

@Entity(tableName = "people_dues")
data class PeopleDueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val initial: String,
    val amountOwed: Double, // Amount user owes this person
    val amountReceivable: Double = 0.0 // Amount this person owes user
)

@Entity(tableName = "tracker_records")
data class TrackerRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val isExpense: Boolean = true,
    val dateString: String,
    val category: String = "বাজার",
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
