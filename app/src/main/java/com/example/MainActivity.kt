package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BazarItemEntity
import com.example.data.NoteEntity
import com.example.data.PeopleDueEntity
import com.example.data.TrackerRecordEntity
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MenegerTheme
import com.example.ui.theme.PrimaryBlue

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Firebase fallback handling
        }
        setContent {
            MenegerTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val dues by viewModel.dues.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val trackerRecords by viewModel.trackerRecords.collectAsStateWithLifecycle()
    val bazarItems by viewModel.bazarItems.collectAsStateWithLifecycle()
    val budgetExpenses by viewModel.budgetExpenses.collectAsStateWithLifecycle()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isSigningIn by viewModel.isSigningIn.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    var showAuthSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedBazarItemForSpent by remember { mutableStateOf<BazarItemEntity?>(null) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var editingDue by remember { mutableStateOf<PeopleDueEntity?>(null) }
    var deletingDue by remember { mutableStateOf<PeopleDueEntity?>(null) }
    var editingTrackerRecord by remember { mutableStateOf<com.example.data.TrackerRecordEntity?>(null) }
    var selectedPersonForTx by remember { mutableStateOf<PeopleDueEntity?>(null) }
    var selectedTxForPayment by remember { mutableStateOf<Pair<com.example.data.PersonTransactionEntity, Double>?>(null) }

    Scaffold(
        topBar = {
            MenegerHeader(
                currentUser = currentUser,
                onMenuClick = { showAuthSheet = true }
            )
        },
        bottomBar = {
            MenegerBottomBar(
                selectedTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Item"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                AppTab.NOTEBOOK -> {
                    NotebookScreen(
                        notes = notes,
                        onEditNote = { editingNote = it },
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onToggleCheckitem = { note, index -> viewModel.toggleNoteChecklistItem(note, index) }
                    )
                }
                AppTab.ACCOUNT -> {
                    AccountScreen(
                        dues = dues,
                        transactions = transactions,
                        payments = payments,
                        onEditDue = { editingDue = it },
                        onDeleteDue = { deletingDue = it },
                        onAddTransaction = { person -> selectedPersonForTx = person },
                        onAddPayment = { tx, remaining -> selectedTxForPayment = Pair(tx, remaining) }
                    )
                }
                AppTab.TRACKER -> {
                    TrackerScreen(
                        records = trackerRecords,
                        onEditRecord = { record ->
                            editingTrackerRecord = record
                        },
                        onAddRecord = {
                            showAddDialog = true
                        }
                    )
                }
                AppTab.BAZAR -> {
                    BazarScreen(
                        bazarItems = bazarItems,
                        onRecordSpent = { selectedBazarItemForSpent = it },
                        onUpdateSpent = { item, newSpent ->
                            viewModel.updateBazarSpent(item, newSpent)
                        },
                        onSwapItems = { item1, item2 ->
                            viewModel.swapBazarItems(item1, item2)
                        }
                    )
                }
                AppTab.BUDGET -> {
                    BudgetScreen(
                        expenses = budgetExpenses,
                        onDeleteExpense = { viewModel.deleteBudgetExpense(it) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        when (currentTab) {
            AppTab.NOTEBOOK -> {
                AddNoteDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { title, content, checklistItems ->
                        viewModel.addNote(title, content, checklistItems)
                    }
                )
            }
            AppTab.ACCOUNT -> {
                AddDueDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name ->
                        viewModel.addDue(name)
                    }
                )
            }
            AppTab.TRACKER -> {
                val existingCategories = remember(trackerRecords) {
                    trackerRecords.map { it.category }.filter { it.isNotBlank() && it != "Uncategorized" }.distinct()
                }
                AddTrackerRecordDialog(
                    existingCategories = existingCategories,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { title, amount, isExpense, category, description, year, month, day, dateString ->
                        viewModel.addTrackerRecord(
                            title = title,
                            amount = amount,
                            isExpense = isExpense,
                            category = category,
                            description = description,
                            year = year,
                            month = month,
                            day = day,
                            dateString = dateString
                        )
                    }
                )
            }
            AppTab.BAZAR -> {
                AddBazarItemDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { title, targetPrice, unitQuantity ->
                        viewModel.addBazarItem(title, targetPrice, unitQuantity)
                    }
                )
            }
            AppTab.BUDGET -> {
                AddBudgetExpenseDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { title, tag, amount ->
                        viewModel.addBudgetExpense(title, tag, amount)
                    }
                )
            }
        }
    }

    selectedBazarItemForSpent?.let { item ->
        EditBazarSpentDialog(
            item = item,
            onDismiss = { selectedBazarItemForSpent = null },
            onConfirm = { newSpent ->
                viewModel.updateBazarSpent(item, newSpent)
            }
        )
    }

    editingNote?.let { note ->
        EditNoteDialog(
            note = note,
            onDismiss = { editingNote = null },
            onConfirm = { updatedNote ->
                viewModel.updateNote(updatedNote)
            }
        )
    }

    editingDue?.let { due ->
        EditDueDialog(
            personName = due.name,
            onDismiss = { editingDue = null },
            onConfirm = { newName ->
                viewModel.updateDueName(due, newName)
            }
        )
    }

    deletingDue?.let { due ->
        ConfirmDeletePersonDialog(
            personName = due.name,
            onDismiss = { deletingDue = null },
            onConfirm = {
                viewModel.deleteDue(due)
            }
        )
    }

    editingTrackerRecord?.let { record ->
        val existingCategories = remember(trackerRecords) {
            trackerRecords.map { it.category }.filter { it.isNotBlank() && it != "Uncategorized" }.distinct()
        }
        EditTrackerRecordDialog(
            record = record,
            existingCategories = existingCategories,
            onDismiss = { editingTrackerRecord = null },
            onConfirm = { updated ->
                viewModel.updateTrackerRecord(updated)
            },
            onDelete = { toDelete ->
                viewModel.deleteTrackerRecord(toDelete)
            }
        )
    }

    selectedPersonForTx?.let { person ->
        AddPersonTransactionDialog(
            personName = person.name,
            onDismiss = { selectedPersonForTx = null },
            onConfirm = { details, amount, isGive ->
                viewModel.addPersonTransaction(person.id, details, amount, isGive)
            }
        )
    }

    selectedTxForPayment?.let { (tx, remaining) ->
        AddPaymentDialog(
            transactionDetails = tx.details,
            remainingAmount = remaining,
            onDismiss = { selectedTxForPayment = null },
            onConfirm = { paidAmount ->
                viewModel.addTransactionPayment(tx.id, tx.personId, paidAmount)
            }
        )
    }

    if (showAuthSheet) {
        AuthMenuBottomSheet(
            currentUser = currentUser,
            isSigningIn = isSigningIn,
            errorMessage = authError,
            onDismiss = { showAuthSheet = false },
            onGoogleSignInClick = {
                viewModel.signInWithGoogle(context)
            },
            onSignOutClick = {
                viewModel.signOut()
            },
            onClearError = {
                viewModel.clearAuthError()
            }
        )
    }
}
