package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BazarItemEntity
import com.example.data.NoteEntity
import com.example.data.PeopleDueEntity
import com.example.data.TrackerRecordEntity

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Note", fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content / Details") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, content)
                        onDismiss()
                    }
                }
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
fun AddDueDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add People Due", fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Person Name (e.g. রানা দোকানদার)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount You Owe (৳)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onConfirm(name, amount)
                        onDismiss()
                    }
                }
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
fun AddTrackerRecordDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, isExpense: Boolean, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("বাজার") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense Record", fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Record Title (e.g. মাথার তেল)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (৳)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. বাজার)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank()) {
                        onConfirm(title, amount, true, category)
                        onDismiss()
                    }
                }
            ) {
                Text("Save Record")
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
    var unitQuantity by remember { mutableStateOf("1 কেজি") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bazar Item", fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Item Name (e.g. শসা)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Price (৳)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unitQuantity,
                    onValueChange = { unitQuantity = it },
                    label = { Text("Unit / Quantity (e.g. 1 কেজি)") },
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
                    label = { Text("Expense Title (e.g. রুম ভাড়া)") },
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
