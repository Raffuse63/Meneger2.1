package com.example.data.export

import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import java.util.Date
import java.util.Locale

object ExportHelper {

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return sdf.format(Date(timestamp))
    }

    fun generateCsv(
        expenses: List<ExpenseEntity>,
        categories: List<CategoryEntity>,
        currencySymbol: String
    ): String {
        val categoryMap = categories.associateBy { it.id }
        val sb = StringBuilder()
        sb.append("ID,বিবরণ,পরিমাণ ($currencySymbol),তারিখ,ক্যাটাগরি\n")
        expenses.forEach { exp ->
            val catName = categoryMap[exp.categoryId]?.name ?: "অজানা"
            val cleanDesc = exp.description.replace(",", " ").replace("\n", " ")
            sb.append("${exp.id},\"$cleanDesc\",${exp.amount},\"${formatDate(exp.date)}\",\"$catName\"\n")
        }
        return sb.toString()
    }

    fun generateFormattedSummary(
        expenses: List<ExpenseEntity>,
        categories: List<CategoryEntity>,
        currencySymbol: String
    ): String {
        val categoryMap = categories.associateBy { it.id }
        val totalSpent = expenses.sumOf { it.amount }
        val totalBudget = categories.sumOf { it.budget }

        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("     অ্যাডভান্সড খরচ ট্র্যাকার রিপোর্ট     \n")
        sb.append("=========================================\n\n")
        sb.append("প্রতিবেদন তৈরির তারিখ: ${formatDate(System.currentTimeMillis())}\n")
        sb.append("মোট ক্যাটাগরি: ${categories.size}\n")
        sb.append("মোট খরচ রেকর্ড: ${expenses.size}\n")
        sb.append("মোট বাজেট: $currencySymbol %.2f\n".format(totalBudget))
        sb.append("মোট খরচ: $currencySymbol %.2f\n".format(totalSpent))
        sb.append("অবশিষ্ট ব্যালেন্স: $currencySymbol %.2f\n\n".format(totalBudget - totalSpent))

        sb.append("--- ক্যাটাগরি অনুসারে বাজেট ও খরচ ---\n")
        categories.forEach { cat ->
            val catExpenses = expenses.filter { it.categoryId == cat.id }
            val catSpent = catExpenses.sumOf { it.amount }
            sb.append("• ${cat.name}: বাজেট $currencySymbol %.2f | খরচ $currencySymbol %.2f\n".format(cat.budget, catSpent))
        }

        sb.append("\n--- লেনদেনের বিস্তারিত বিবরণ ---\n")
        expenses.forEachIndexed { index, exp ->
            val catName = categoryMap[exp.categoryId]?.name ?: "অজানা"
            sb.append("${index + 1}. ${exp.description}\n")
            sb.append("   পরিমাণ: $currencySymbol %.2f | ক্যাটাগরি: $catName\n".format(exp.amount))
            sb.append("   তারিখ: ${formatDate(exp.date)}\n")
        }
        sb.append("\n=========================================\n")
        return sb.toString()
    }

    fun shareTextData(context: Context, title: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        val chooser = Intent.createChooser(intent, title)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
