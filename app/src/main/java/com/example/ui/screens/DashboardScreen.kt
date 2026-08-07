package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.ui.components.*
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onCategoryClick: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val filteredExpenses by viewModel.filteredExpenses.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var deletingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var movingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var isReorderMode by remember { mutableStateOf(false) }

    // Metrics calculation according to selected category filter
    val selectedCatId = userPrefs.selectedCategoryFilter
    val currency = userPrefs.currencySymbol

    val targetCategories = remember(categories, selectedCatId) {
        if (selectedCatId == -1L) categories else categories.filter { it.id == selectedCatId }
    }

    val totalBudget = remember(targetCategories) {
        targetCategories.sumOf { it.budget }
    }

    val totalSpent = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amount }
    }

    val remainingBalance = totalBudget - totalSpent

    val percentageUsed = remember(totalBudget, totalSpent) {
        if (totalBudget > 0) ((totalSpent / totalBudget) * 100).toFloat() else 0f
    }

    val categoryMap = remember(categories) {
        categories.associateBy { it.id }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                containerColor = Color(0xFFD0E3FF),
                contentColor = Color(0xFF001D36),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("add_expense_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "নতুন খরচ যোগ করুন",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp)
        ) {
            // Summary Balance Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("summary_balance_card"),
                    shape = RoundedCornerShape(28.dp),
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
                                    text = "মোট বাজেট",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "৳ %,d".format(totalBudget.toLong()),
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
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable { onCategoryClick() }
                                    .testTag("summary_category_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = "ক্যাটাগরি",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Middle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "মোট খরচ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "৳ %,d".format(totalSpent.toLong()),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "অবশিষ্ট ব্যালেন্স",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "৳ %,d".format(remainingBalance.toLong()),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
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
                                    text = "ব্যবহৃত হয়েছে",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "${percentageUsed.toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (percentageUsed / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = Color(0xFF5A9BFF),
                                trackColor = Color(0xFF00447C)
                            )
                        }
                    }
                }
            }

            // Recent Transactions Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isReorderMode) "সাজানোর মুড (উপরে/নিচে নামান)" else "সাম্প্রতিক খরচ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isReorderMode) Color(0xFFD97706) else Color(0xFF1E293B),
                        modifier = Modifier.weight(1f)
                    )

                    if (filteredExpenses.isNotEmpty()) {
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
            }

            // Transactions List
            if (filteredExpenses.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "কোনো খরচ পাওয়া যায়নি",
                        subtitle = "নতুন খরচ যোগ করতে নিচের '+' বাটনে ট্যাপ করুন"
                    )
                }
            } else {
                val expenseList = filteredExpenses.take(10)
                itemsIndexed(
                    items = expenseList,
                    key = { _, expense -> expense.id }
                ) { index, expense ->
                    val catName = categoryMap[expense.categoryId]?.name ?: "অজানা ক্যাটাগরি"
                    val catIndex = categories.indexOfFirst { it.id == expense.categoryId }.coerceAtLeast(0)

                    ExpenseItemCard(
                        expense = expense,
                        categoryName = catName,
                        currencySymbol = currency,
                        categoryIndex = catIndex,
                        onEdit = { editingExpense = expense },
                        onDelete = { deletingExpense = expense },
                        onMove = { movingExpense = expense },
                        isReorderMode = isReorderMode,
                        canMoveUp = index > 0,
                        canMoveDown = index < expenseList.size - 1,
                        onMoveUp = { viewModel.moveExpenseUp(expense, expenseList) },
                        onMoveDown = { viewModel.moveExpenseDown(expense, expenseList) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showAddExpenseDialog) {
        AddEditExpenseDialog(
            categories = categories,
            currencySymbol = currency,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { desc, amount, date, catId ->
                viewModel.addExpense(desc, amount, date, catId) {
                    showAddExpenseDialog = false
                }
            }
        )
    }

    editingExpense?.let { expense ->
        AddEditExpenseDialog(
            initialExpense = expense,
            categories = categories,
            currencySymbol = currency,
            onDismiss = { editingExpense = null },
            onSave = { desc, amount, date, catId ->
                viewModel.editExpense(
                    expense.copy(description = desc, amount = amount, date = date, categoryId = catId)
                ) {
                    editingExpense = null
                }
            }
        )
    }

    deletingExpense?.let { expense ->
        ConfirmationDialog(
            title = "খরচ মুছে ফেলা",
            message = "\"${expense.description}\" খরচ রেকর্ডটি স্থায়ীভাবে মুছে ফেলা হবে। আপনি কি নিশ্চিত?",
            onConfirm = {
                viewModel.deleteExpense(expense)
                deletingExpense = null
            },
            onDismiss = { deletingExpense = null }
        )
    }

    movingExpense?.let { expense ->
        MoveCategoryDialog(
            expense = expense,
            categories = categories,
            onDismiss = { movingExpense = null },
            onMove = { newCatId ->
                viewModel.moveExpenseCategory(expense.id, newCatId)
                movingExpense = null
            }
        )
    }
}
