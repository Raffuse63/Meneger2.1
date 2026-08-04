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
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val dues by viewModel.dues.collectAsStateWithLifecycle()
    val trackerRecords by viewModel.trackerRecords.collectAsStateWithLifecycle()
    val bazarItems by viewModel.bazarItems.collectAsStateWithLifecycle()
    val budgetExpenses by viewModel.budgetExpenses.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedBazarItemForSpent by remember { mutableStateOf<BazarItemEntity?>(null) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var editingDue by remember { mutableStateOf<PeopleDueEntity?>(null) }

    Scaffold(
        topBar = {
            MenegerHeader()
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
                        onDeleteNote = { viewModel.deleteNote(it) }
                    )
                }
                AppTab.ACCOUNT -> {
                    AccountScreen(
                        dues = dues,
                        onEditDue = { editingDue = it }
                    )
                }
                AppTab.TRACKER -> {
                    TrackerScreen(
                        records = trackerRecords,
                        onEditRecord = { record ->
                            // Delete or update
                        }
                    )
                }
                AppTab.BAZAR -> {
                    BazarScreen(
                        bazarItems = bazarItems,
                        onRecordSpent = { selectedBazarItemForSpent = it }
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
                    onConfirm = { title, content ->
                        viewModel.addNote(title, content)
                    }
                )
            }
            AppTab.ACCOUNT -> {
                AddDueDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name, amount ->
                        viewModel.addDue(name, amount)
                    }
                )
            }
            AppTab.TRACKER -> {
                AddTrackerRecordDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { title, amount, isExpense, category ->
                        viewModel.addTrackerRecord(title, amount, isExpense, category)
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
}
