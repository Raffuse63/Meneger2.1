package com.example.ui.components

import android.icu.text.SimpleDateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.ui.theme.*
import java.util.*

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmButtonText: String = "হ্যাঁ, নিশ্চিত",
    dismissButtonText: String = "বাতিল",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "সতর্কতা",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("confirm_dialog_action")
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("confirm_dialog_dismiss")
            ) {
                Text(dismissButtonText)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterDropdown(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedName = if (selectedCategoryId == -1L) {
        "সব ক্যাটাগরি"
    } else {
        categories.find { it.id == selectedCategoryId }?.name ?: "সব ক্যাটাগরি"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.testTag("category_filter_dropdown")
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("ফিল্টার করুন") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "ফিল্টার"
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("সব ক্যাটাগরি (ALL)", fontWeight = FontWeight.Medium)
                    }
                },
                onClick = {
                    onCategorySelected(-1L)
                    expanded = false
                },
                modifier = Modifier.testTag("filter_option_all")
            )

            HorizontalDivider()

            categories.forEachIndexed { index, category ->
                val color = CategoryColors[index % CategoryColors.size]
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(category.name)
                        }
                    },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    },
                    modifier = Modifier.testTag("filter_option_${category.id}")
                )
            }
        }
    }
}

@Composable
fun AnimatedBudgetProgressBar(
    percentage: Float,
    modifier: Modifier = Modifier
) {
    val clampedPercentage = percentage.coerceIn(0f, 100f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedPercentage / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )

    // Progress colors: 0–30% Green, 31–80% Blue, Above 80% Red
    val targetColor = when {
        clampedPercentage <= 30f -> ProgressGreen
        clampedPercentage <= 80f -> ProgressBlue
        else -> ExpenseRed
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500),
        label = "color"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ব্যবহৃত শতাংশ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "%.1f%%".format(percentage),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = animatedColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = animatedColor,
            trackColor = animatedColor.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun ExpenseItemCard(
    expense: ExpenseEntity,
    categoryName: String,
    currencySymbol: String,
    categoryIndex: Int = 0,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    isReorderMode: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (bgColor, iconColor, icon) = when {
        categoryName.contains("খাবার") || categoryName.lowercase().contains("food") -> Triple(PastelFoodBg, PastelFoodIcon, Icons.Default.Restaurant)
        categoryName.contains("যাতায়াত") || categoryName.contains("ট্রান্সপোর্ট") || categoryName.lowercase().contains("transport") -> Triple(PastelTransportBg, PastelTransportIcon, Icons.Default.DirectionsCar)
        categoryName.contains("শপিং") || categoryName.lowercase().contains("shopping") -> Triple(PastelShoppingBg, PastelShoppingIcon, Icons.Default.ShoppingBag)
        categoryName.contains("বিল") || categoryName.lowercase().contains("bills") -> Triple(PastelBillsBg, PastelBillsIcon, Icons.Default.Receipt)
        else -> {
            val color = CategoryColors[categoryIndex % CategoryColors.size]
            Triple(color.copy(alpha = 0.15f), color, Icons.Default.Receipt)
        }
    }

    val formattedAmount = remember(expense.amount) {
        val longVal = expense.amount.toLong()
        if (expense.amount == longVal.toDouble()) {
            "৳ %,d".format(longVal)
        } else {
            "৳ %,.2f".format(expense.amount)
        }
    }

    val formattedDate = remember(expense.date) {
        val now = Calendar.getInstance()
        val expenseCal = Calendar.getInstance().apply { timeInMillis = expense.date }

        val isToday = now.get(Calendar.YEAR) == expenseCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == expenseCal.get(Calendar.DAY_OF_YEAR)

        val isYesterday = now.get(Calendar.YEAR) == expenseCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - expenseCal.get(Calendar.DAY_OF_YEAR) == 1

        when {
            isToday -> "আজ"
            isYesterday -> "গতকাল"
            else -> SimpleDateFormat("dd MMM", Locale.US).format(Date(expense.date))
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("expense_item_${expense.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = categoryName,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = iconColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "•  $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "- $formattedAmount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            }

            if (isReorderMode) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(start = 8.dp)
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseDialog(
    initialExpense: ExpenseEntity? = null,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    defaultDate: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit,
    onSave: (description: String, amount: Double, date: Long, categoryId: Long) -> Unit
) {
    var description by remember { mutableStateOf(initialExpense?.description ?: "") }
    var amountText by remember { mutableStateOf(initialExpense?.amount?.let { if (it > 0) it.toString() else "" } ?: "") }
    var selectedCategoryId by remember { mutableStateOf(initialExpense?.categoryId ?: categories.firstOrNull()?.id ?: -1L) }
    var dateMillis by remember { mutableStateOf(initialExpense?.date ?: defaultDate) }

    var descriptionError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }

    var expandedCatDropdown by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("dd MMM, yyyy", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialExpense == null) "নতুন খরচ যোগ করুন" else "খরচ সম্পাদনা করুন",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        if (it.trim().isNotEmpty()) descriptionError = null
                    },
                    label = { Text("বিবরণ *") },
                    placeholder = { Text("যেমন: সুপারশপ বাজার") },
                    isError = descriptionError != null,
                    supportingText = descriptionError?.let { { Text(it) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_desc_input")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if (it.toDoubleOrNull()?.let { amt -> amt > 0 } == true) amountError = null
                    },
                    label = { Text("পরিমাণ ($currencySymbol) *") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                // Category Selection Dropdown
                val currentCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: "ক্যাটাগরি নির্বাচন করুন"

                ExposedDropdownMenuBox(
                    expanded = expandedCatDropdown,
                    onExpandedChange = { expandedCatDropdown = !expandedCatDropdown }
                ) {
                    OutlinedTextField(
                        value = currentCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ক্যাটাগরি *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCatDropdown) },
                        isError = categoryError != null,
                        supportingText = categoryError?.let { { Text(it) } },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                            .testTag("expense_category_selector")
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCatDropdown,
                        onDismissRequest = { expandedCatDropdown = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryError = null
                                    expandedCatDropdown = false
                                }
                            )
                        }
                    }
                }

                // Date Display
                OutlinedTextField(
                    value = sdf.format(Date(dateMillis)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("তারিখ") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "তারিখ") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var isValid = true
                    if (description.trim().isEmpty()) {
                        descriptionError = "বিবরণ খালি রাখা যাবে না"
                        isValid = false
                    }
                    val parsedAmt = amountText.toDoubleOrNull()
                    if (parsedAmt == null || parsedAmt <= 0) {
                        amountError = "পরিমাণ ০ এর চেয়ে বেশি হতে হবে"
                        isValid = false
                    }
                    if (selectedCategoryId <= 0) {
                        categoryError = "একটি ক্যাটাগরি নির্বাচন করুন"
                        isValid = false
                    }

                    if (isValid && parsedAmt != null) {
                        onSave(description.trim(), parsedAmt, dateMillis, selectedCategoryId)
                    }
                },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun AddEditCategoryDialog(
    initialCategory: CategoryEntity? = null,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, budget: Double) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var budgetText by remember { mutableStateOf(initialCategory?.budget?.let { if (it > 0) it.toString() else "" } ?: "") }

    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCategory == null) "নতুন ক্যাটাগরি তৈরি করুন" else "ক্যাটাগরি সম্পাদনা করুন",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.trim().isNotEmpty()) nameError = null
                    },
                    label = { Text("ক্যাটাগরির নাম *") },
                    placeholder = { Text("যেমন: বিনোদন") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text("বাজেট পরিমাণ ($currencySymbol) (ঐচ্ছিক)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_budget_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        nameError = "ক্যাটাগরির নাম খালি রাখা যাবে না"
                        return@Button
                    }
                    val budget = budgetText.toDoubleOrNull() ?: 0.0
                    onSave(name.trim(), budget)
                },
                modifier = Modifier.testTag("save_category_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveCategoryDialog(
    expense: ExpenseEntity,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onMove: (newCategoryId: Long) -> Unit
) {
    var selectedCatId by remember { mutableStateOf(expense.categoryId) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("অন্য ক্যাটাগরিতে সরান", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    text = "আইটেম: ${expense.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))

                val selectedName = categories.find { it.id == selectedCatId }?.name ?: "নির্বাচন করুন"

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("নতুন ক্যাটাগরি") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCatId = cat.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onMove(selectedCatId) },
                enabled = selectedCatId != expense.categoryId
            ) {
                Text("সরান")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.ReceiptLong,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
