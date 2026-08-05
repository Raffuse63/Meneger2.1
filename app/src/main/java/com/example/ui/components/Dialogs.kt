package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RedAccent
import com.example.ui.theme.TextPrimary

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, checklistItems: List<NoteChecklistItem>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val checklistItems = remember { mutableStateListOf<NoteChecklistItem>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Note", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Details / Note content") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                // Checklist Section
                ChecklistInputSection(checklistItems = checklistItems)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() || content.isNotBlank() || checklistItems.isNotEmpty()) {
                        val finalTitle = if (title.isBlank()) "New Note" else title
                        onConfirm(finalTitle, content, checklistItems.toList())
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditNoteDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onConfirm: (updatedNote: NoteEntity) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    val checklistItems = remember {
        mutableStateListOf<NoteChecklistItem>().apply {
            addAll(note.getChecklistItems())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Note", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Details / Note content") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                // Checklist Section
                ChecklistInputSection(checklistItems = checklistItems)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() || content.isNotBlank() || checklistItems.isNotEmpty()) {
                        val updated = note.copy(
                            title = if (title.isBlank()) "Note" else title,
                            content = content,
                            checklistJson = NoteChecklistSerializer.serialize(checklistItems.toList())
                        )
                        onConfirm(updated)
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ChecklistInputSection(
    checklistItems: SnapshotStateList<NoteChecklistItem>
) {
    var newItemText by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editingText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "📋 Checklist",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Add item row with + button beside input field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                label = { Text("Add new item...") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (newItemText.isNotBlank()) {
                        checklistItems.add(NoteChecklistItem(text = newItemText.trim(), isChecked = false))
                        newItemText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryBlue)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Checklist Item",
                    tint = Color.White
                )
            }
        }

        // Added checklist items list
        if (checklistItems.isNotEmpty()) {
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    checklistItems.forEachIndexed { index, item ->
                        if (editingIndex == index) {
                            // Inline editing row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = editingText,
                                    onValueChange = { editingText = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                IconButton(
                                    onClick = {
                                        if (editingText.isNotBlank()) {
                                            checklistItems[index] = item.copy(text = editingText.trim())
                                        }
                                        editingIndex = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save Edit",
                                        tint = PrimaryBlue
                                    )
                                }
                                IconButton(
                                    onClick = { editingIndex = null }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Edit",
                                        tint = RedAccent
                                    )
                                }
                            }
                        } else {
                            // Normal item row with edit & delete buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "•",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = item.text,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        editingIndex = index
                                        editingText = item.text
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Item",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { checklistItems.removeAt(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Item",
                                        tint = RedAccent,
                                        modifier = Modifier.size(16.dp)
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

@Composable
fun AddDueDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Person", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Person Name (e.g. John)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim())
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditDueDialog(
    personName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(personName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Person Name", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Person Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim())
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ConfirmDeletePersonDialog(
    personName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Person", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Text("Are you sure you want to delete '$personName'? All associated transaction history will also be deleted.")
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddTrackerRecordDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        amount: Double,
        isExpense: Boolean,
        category: String,
        description: String,
        year: Int,
        month: String,
        day: String,
        dateString: String
    ) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val calendar = remember { java.util.Calendar.getInstance() }

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Groceries") }
    var description by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }

    var year by remember { mutableIntStateOf(calendar.get(java.util.Calendar.YEAR)) }
    var monthIndex by remember { mutableIntStateOf(calendar.get(java.util.Calendar.MONTH)) }
    var dayOfMonth by remember { mutableIntStateOf(calendar.get(java.util.Calendar.DAY_OF_MONTH)) }

    val monthsList = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    var dateDisplayString by remember {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
        mutableStateOf(sdf.format(calendar.time))
    }

    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, pickedYear, pickedMonth, pickedDay ->
                year = pickedYear
                monthIndex = pickedMonth
                dayOfMonth = pickedDay

                val cal = java.util.Calendar.getInstance()
                cal.set(pickedYear, pickedMonth, pickedDay)
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
                dateDisplayString = sdf.format(cal.time)
            },
            year,
            monthIndex,
            dayOfMonth
        )
    }

    val sampleCategories = listOf("Groceries", "Personal", "House Rent", "Food", "Transport", "Others")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Record", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type selector: Expense / Income
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        label = { Text("💸 Expense") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        label = { Text("💰 Income") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Title and Amount in first row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Category and Date Picker in second row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Pick Date",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateDisplayString,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Quick category suggestions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sampleCategories.forEach { cat ->
                        SuggestionChip(
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                // Description in third row
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        val monthName = monthsList.getOrElse(monthIndex) { "August" }
                        onConfirm(
                            title.trim(),
                            amount,
                            isExpense,
                            category.ifBlank { "Others" },
                            description.trim(),
                            year,
                            monthName,
                            dayOfMonth.toString(),
                            dateDisplayString
                        )
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditTrackerRecordDialog(
    record: com.example.data.TrackerRecordEntity,
    onDismiss: () -> Unit,
    onConfirm: (updatedRecord: com.example.data.TrackerRecordEntity) -> Unit,
    onDelete: (record: com.example.data.TrackerRecordEntity) -> Unit
) {
    var title by remember { mutableStateOf(record.title) }
    var amountText by remember { mutableStateOf(record.amount.toInt().toString()) }
    var category by remember { mutableStateOf(record.category) }
    var description by remember { mutableStateOf(record.description) }
    var isExpense by remember { mutableStateOf(record.isExpense) }

    val sampleCategories = listOf("Groceries", "Personal", "House Rent", "Food", "Transport", "Others")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Record", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        label = { Text("💸 Expense") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        label = { Text("💰 Income") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Title and Amount in first row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Category and Date info in second row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = record.dateString,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Quick category suggestions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sampleCategories.forEach { cat ->
                        SuggestionChip(
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                // Description in third row
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        onDelete(record)
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = RedAccent, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amount > 0) {
                            onConfirm(
                                record.copy(
                                    title = title.trim(),
                                    amount = amount,
                                    isExpense = isExpense,
                                    category = category.ifBlank { "Others" },
                                    description = description.trim()
                                )
                            )
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Update")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddBazarItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, targetPrice: Double, unitQuantity: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var unitQuantity by remember { mutableStateOf("1 kg") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bazar Item", fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Item Name (e.g. Cucumber)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Price (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unitQuantity,
                    onValueChange = { unitQuantity = it },
                    label = { Text("Unit / Quantity (e.g. 1 kg)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank()) {
                        onConfirm(title, target, unitQuantity)
                        onDismiss()
                    }
                }
            ) {
                Text("Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditBazarSpentDialog(
    item: BazarItemEntity,
    onDismiss: () -> Unit,
    onConfirm: (spent: Double) -> Unit
) {
    var spentText by remember { mutableStateOf(if (item.actualSpent > 0) item.actualSpent.toInt().toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Expense for ${item.title}", fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Target: ৳${item.targetPrice.toInt()} (${item.unitQuantity})", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = spentText,
                    onValueChange = { spentText = it },
                    label = { Text("Actual Amount Spent (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val spent = spentText.toDoubleOrNull() ?: 0.0
                    onConfirm(spent)
                    onDismiss()
                }
            ) {
                Text("Save Spent Amount")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddBudgetExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, tag: String, amount: Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("Payslip") }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Budget Expense", fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title (e.g. Room Rent)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Category / Tag (e.g. Payslip)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank()) {
                        onConfirm(title, tag, amount)
                        onDismiss()
                    }
                }
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddPersonTransactionDialog(
    personName: String,
    onDismiss: () -> Unit,
    onConfirm: (details: String, amount: Double, isGive: Boolean) -> Unit
) {
    var details by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isGive by remember { mutableStateOf(true) } // true = I give (Receivable +), false = I receive (Payable -)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$personName - New Transaction", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Transaction Type:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // I give Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isGive = true },
                        color = if (isGive) Color(0xFF16A34A) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isGive) Color(0xFF16A34A) else Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "I give",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGive) Color.White else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Receivable (+)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isGive) Color.White.copy(alpha = 0.9f) else Color(0xFF16A34A)
                            )
                        }
                    }

                    // I receive Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isGive = false },
                        color = if (!isGive) Color(0xFFDC2626) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isGive) Color(0xFFDC2626) else Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "I receive",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isGive) Color.White else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Payable (-)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (!isGive) Color.White.copy(alpha = 0.9f) else Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (details.isNotBlank() && amt != null && amt > 0) {
                        onConfirm(details.trim(), amt, isGive)
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("+ Add transaction", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddPaymentDialog(
    transactionDetails: String,
    remainingAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (paidAmount: Double) -> Unit
) {
    var paidAmountText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
    val remainingFormatted = "৳${formatter.format(remainingAmount.toInt())}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay Amount", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Details: $transactionDetails",
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Remaining Balance: $remainingFormatted",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )

                OutlinedTextField(
                    value = paidAmountText,
                    onValueChange = {
                        paidAmountText = it
                        errorMessage = null
                    },
                    label = { Text("Pay Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null,
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = RedAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = paidAmountText.toDoubleOrNull()
                    when {
                        amt == null || amt <= 0 -> {
                            errorMessage = "Please enter a valid amount"
                        }
                        amt > remainingAmount -> {
                            errorMessage = "Pay amount should not be greater than Remaining amount ($remainingFormatted)"
                        }
                        else -> {
                            onConfirm(amt)
                            onDismiss()
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Pay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

