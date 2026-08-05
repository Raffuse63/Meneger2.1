package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TrackerRecordEntity
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

data class CategoryGroupRecord(
    val category: String,
    val totalAmount: Double,
    val isExpense: Boolean,
    val items: List<TrackerRecordEntity>
)

data class TitleGroupRecord(
    val title: String,
    val category: String,
    val totalAmount: Double,
    val isExpense: Boolean,
    val items: List<TrackerRecordEntity>
)

data class DailySummaryItem(
    val dateLabel: String,
    val dateSortKey: Long,
    val income: Double,
    val expense: Double,
    val balance: Double
)

@Composable
fun TrackerScreen(
    records: List<TrackerRecordEntity>,
    onEditRecord: (TrackerRecordEntity) -> Unit,
    onAddRecord: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedYear by remember { mutableStateOf("2026") }
    var selectedMonth by remember { mutableStateOf("August") }
    var selectedDay by remember { mutableStateOf("All Days") }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) } // Hidden by default
    var viewMode by remember { mutableStateOf("CATEGORY") } // "CATEGORY" or "TITLE"

    var selectedCategoryFilter by remember { mutableStateOf("All Categories") }
    var selectedTitleFilter by remember { mutableStateOf("All Titles") }

    // Dropdown visibility states
    var showYearDropdown by remember { mutableStateOf(false) }
    var showMonthDropdown by remember { mutableStateOf(false) }
    var showDayDropdown by remember { mutableStateOf(false) }

    val yearsList = remember { (2026..2099).map { it.toString() } }
    val monthsList = remember {
        listOf("All Months", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    }
    val daysList = remember {
        listOf("All Days") + (1..31).map { it.toString() }
    }

    val monthIndexMap = remember {
        mapOf(
            "january" to 1, "february" to 2, "march" to 3, "april" to 4,
            "may" to 5, "june" to 6, "july" to 7, "august" to 8,
            "september" to 9, "october" to 10, "november" to 11, "december" to 12,
            "January" to 1, "February" to 2, "March" to 3, "April" to 4,
            "May" to 5, "June" to 6, "July" to 7, "August" to 8,
            "September" to 9, "October" to 10, "November" to 11, "December" to 12
        )
    }

    // Filtered Records for selected time & search
    val filteredRecords = remember(records, selectedYear, selectedMonth, selectedDay, selectedCategoryFilter, selectedTitleFilter, searchQuery) {
        records.filter { record ->
            val matchYear = record.year.toString() == selectedYear || record.dateString.contains(selectedYear)
            val matchMonth = selectedMonth == "All Months" ||
                    record.month.equals(selectedMonth, ignoreCase = true) ||
                    record.dateString.contains(selectedMonth, ignoreCase = true)
            val matchDay = selectedDay == "All Days" ||
                    record.day == selectedDay ||
                    record.dateString.startsWith(selectedDay) ||
                    record.dateString.startsWith("0$selectedDay")
            val matchCategory = selectedCategoryFilter == "All Categories" ||
                    record.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchTitle = selectedTitleFilter == "All Titles" ||
                    record.title.equals(selectedTitleFilter, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() ||
                    record.title.contains(searchQuery, ignoreCase = true) ||
                    record.category.contains(searchQuery, ignoreCase = true) ||
                    record.description.contains(searchQuery, ignoreCase = true)

            matchYear && matchMonth && matchDay && matchCategory && matchTitle && matchSearch
        }
    }

    // Grouping by Category
    val categoryGroups = remember(filteredRecords) {
        filteredRecords.groupBy { it.category.trim() }
            .map { (categoryName, list) ->
                CategoryGroupRecord(
                    category = categoryName,
                    totalAmount = list.sumOf { it.amount },
                    isExpense = list.any { it.isExpense },
                    items = list
                )
            }.sortedByDescending { it.totalAmount }
    }

    // Grouping by Title
    val titleGroups = remember(filteredRecords) {
        filteredRecords.groupBy { Pair(it.title.trim(), it.category.trim()) }
            .map { (pair, list) ->
                TitleGroupRecord(
                    title = pair.first,
                    category = pair.second,
                    totalAmount = list.sumOf { it.amount },
                    isExpense = list.any { it.isExpense },
                    items = list
                )
            }.sortedByDescending { it.totalAmount }
    }

    val totalIncome = remember(filteredRecords) {
        filteredRecords.filter { !it.isExpense }.sumOf { it.amount }
    }
    val totalExpense = remember(filteredRecords) {
        filteredRecords.filter { it.isExpense }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense

    val expenseRatio = remember(totalIncome, totalExpense) {
        if (totalIncome > 0) {
            ((totalExpense / totalIncome) * 100).toInt().coerceAtMost(100)
        } else if (totalExpense > 0) {
            100
        } else {
            0
        }
    }

    val prevMonthBalance = remember(records, selectedYear, selectedMonth) {
        if (selectedMonth == "All Months" && selectedYear == "All Years") {
            0.0
        } else if (selectedMonth == "All Months") {
            val currYr = selectedYear.toIntOrNull() ?: 2026
            val priorRecords = records.filter { record ->
                record.year < currYr
            }
            val inc = priorRecords.filter { !it.isExpense }.sumOf { it.amount }
            val exp = priorRecords.filter { it.isExpense }.sumOf { it.amount }
            inc - exp
        } else {
            val currMonthIdx = monthIndexMap[selectedMonth]
                ?: monthIndexMap[selectedMonth.lowercase()]
                ?: 8
            val currYr = selectedYear.toIntOrNull() ?: 2026

            val priorRecords = records.filter { record ->
                val rMonthIdx = monthIndexMap[record.month.trim()]
                    ?: monthIndexMap[record.month.trim().lowercase()]
                    ?: 8
                (record.year < currYr) || (record.year == currYr && rMonthIdx < currMonthIdx)
            }
            val inc = priorRecords.filter { !it.isExpense }.sumOf { it.amount }
            val exp = priorRecords.filter { it.isExpense }.sumOf { it.amount }
            inc - exp
        }
    }

    val formatter = NumberFormat.getNumberInstance(Locale.US)

    // Calculate Daily Summary table
    val dailySummaryList = remember(records) {
        val groups = records.groupBy { record ->
            val monthIdx = monthIndexMap[record.month.lowercase()] ?: 8
            val dayInt = record.day.toIntOrNull() ?: 1
            record.year.toLong() * 10000L + monthIdx.toLong() * 100L + dayInt.toLong()
        }

        val sortedKeys = groups.keys.sorted()

        var runningBal = 0.0
        val chronologicalList = sortedKeys.map { key ->
            val list = groups[key] ?: emptyList()
            val firstRec = list.first()
            val dayStr = firstRec.day.toIntOrNull()?.toString() ?: firstRec.day
            val monthName = firstRec.month.ifBlank { "August" }
            val yrShort = (firstRec.year % 100).toString().padStart(2, '0')
            val label = "$dayStr $monthName $yrShort"

            val inc = list.filter { !it.isExpense }.sumOf { it.amount }
            val exp = list.filter { it.isExpense }.sumOf { it.amount }
            runningBal += (inc - exp)

            DailySummaryItem(
                dateLabel = label,
                dateSortKey = key,
                income = inc,
                expense = exp,
                balance = runningBal
            )
        }

        chronologicalList.reversed()
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Top Summary Card (Matching Account, Bazar, and Budget screens)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SummaryCardBlue)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Net Balance",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Balance",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = if (netBalance < 0) "-৳ ${formatter.format(-netBalance.toInt())}" else "৳ ${formatter.format(netBalance.toInt())}",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Income",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "৳ ${formatter.format(totalIncome.toInt())}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Expense",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "৳ ${formatter.format(totalExpense.toInt())}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pre Month",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = if (prevMonthBalance < 0) "-৳ ${formatter.format(-prevMonthBalance.toInt())}" else "৳ ${formatter.format(prevMonthBalance.toInt())}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { (expenseRatio / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // Filter & Search Records Box (Exact design match)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title Header: 🔍 Filter & Search Records
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔍 Filter & Search Records",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Row 1 Filters: [2026 ▼] [August ▼] [All Days ▼] [🔍 Hide / Search Button]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Year Pill
                            Box(modifier = Modifier.weight(1f)) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showYearDropdown = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "$selectedYear  ▼",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showYearDropdown,
                                    onDismissRequest = { showYearDropdown = false }
                                ) {
                                    yearsList.forEach { y ->
                                        DropdownMenuItem(
                                            text = { Text(y) },
                                            onClick = {
                                                selectedYear = y
                                                showYearDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Month Pill
                            Box(modifier = Modifier.weight(1.2f)) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showMonthDropdown = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "$selectedMonth  ▼",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMonthDropdown,
                                    onDismissRequest = { showMonthDropdown = false }
                                ) {
                                    monthsList.forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(m) },
                                            onClick = {
                                                selectedMonth = m
                                                showMonthDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Day Pill
                            Box(modifier = Modifier.weight(1.1f)) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showDayDropdown = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "$selectedDay  ▼",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showDayDropdown,
                                    onDismissRequest = { showDayDropdown = false }
                                ) {
                                    daysList.forEach { d ->
                                        DropdownMenuItem(
                                            text = { Text(d) },
                                            onClick = {
                                                selectedDay = d
                                                showDayDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Blue Toggle Search / Hide Button
                            Button(
                                onClick = { isSearchActive = !isSearchActive },
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSearchActive) "Hide" else "Search",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Expanded Search Panel: Input box + View mode toggle + Expense Card Box
                        AnimatedVisibility(visible = isSearchActive) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Search Input Box ("e.g. food")
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("e.g. food", fontSize = 13.sp, color = TextMuted) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = PrimaryBlue,
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    )
                                )

                                // View Type Selector Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "ভিউ টাইপ:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    // Category List Button
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { viewMode = "CATEGORY" },
                                        color = if (viewMode == "CATEGORY") PrimaryBlue else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "📂 ক্যাটাগরি তালিকা",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (viewMode == "CATEGORY") Color.White else TextPrimary
                                            )
                                        }
                                    }

                                    // Title List Button (Requested: "ছবির গ্রুপ এর স্থানে টাইটেল হবে শুধু")
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { viewMode = "TITLE" },
                                        color = if (viewMode == "TITLE") PrimaryBlue else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "👤 টাইটেল তালিকা",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (viewMode == "TITLE") Color.White else TextPrimary
                                            )
                                        }
                                    }
                                }

                                // Category or Title Expense Summary Box
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (viewMode == "CATEGORY") "📂 ক্যাটাগরি খরচ:" else "👤 টাইটেল খরচ:",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )

                                        if (viewMode == "CATEGORY") {
                                            if (categoryGroups.isEmpty()) {
                                                Text(
                                                    text = "কোনো রেকর্ড পাওয়া যায়নি",
                                                    fontSize = 12.sp,
                                                    color = TextMuted,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            } else {
                                                categoryGroups.forEach { catGroup ->
                                                    ExpensePillRow(
                                                        name = catGroup.category,
                                                        amountStr = "৳${formatter.format(catGroup.totalAmount.toInt())}"
                                                    )
                                                }
                                            }
                                        } else {
                                            if (titleGroups.isEmpty()) {
                                                Text(
                                                    text = "কোনো রেকর্ড পাওয়া যায়নি",
                                                    fontSize = 12.sp,
                                                    color = TextMuted,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            } else {
                                                titleGroups.forEach { titleGroup ->
                                                    ExpensePillRow(
                                                        name = titleGroup.title,
                                                        amountStr = "৳${formatter.format(titleGroup.totalAmount.toInt())}"
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
            }

            // Records List Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 Records List",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Records List Items
            if (filteredRecords.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📋", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "কোনো রেকর্ড নেই",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+ বাটনে চাপ দিয়ে নতুন রেকর্ড যোগ করুন",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                items(filteredRecords, key = { it.id }) { record ->
                    RecordListItemCard(
                        record = record,
                        formatter = formatter,
                        onEdit = { onEditRecord(record) }
                    )
                }
            }

            // Daily Summary Section Card (as in screenshot)
            item {
                DailySummaryCard(
                    summaryList = dailySummaryList,
                    formatter = formatter
                )
            }
        }

        // Floating Action Button (+) at bottom right
        FloatingActionButton(
            onClick = onAddRecord,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = PrimaryBlue,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Record",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ExpensePillRow(
    name: String,
    amountStr: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            Text(
                text = amountStr,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = RedAccent
            )
        }
    }
}

@Composable
private fun RecordListItemCard(
    record: TrackerRecordEntity,
    formatter: NumberFormat,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left indicator bar (Green for income, Red for expense)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(68.dp)
                    .background(
                        if (record.isExpense) RedAccent else GreenAccent,
                        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Details column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.dateString,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "👤 ${record.category}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }

                // Amount & Edit pencil button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "৳${formatter.format(record.amount.toInt())}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (record.isExpense) RedAccent else GreenAccent
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailySummaryCard(
    summaryList: List<DailySummaryItem>,
    formatter: NumberFormat
) {
    var isExpanded by remember { mutableStateOf(false) }
    val visibleItems = if (isExpanded) summaryList else summaryList.take(7)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: [📅 Daily Summary]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "📅", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Daily Summary",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Column Headers: Date | Income | Expense | Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = "Income",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15803D),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                Text(
                    text = "Expense",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB91C1C),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                Text(
                    text = "Balance",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1.1f),
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            if (summaryList.isEmpty()) {
                Text(
                    text = "No daily summary data available",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                visibleItems.forEach { item ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.dateLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                text = if (item.income > 0) "৳${formatter.format(item.income.toInt())}" else "0",
                                fontSize = 13.sp,
                                fontWeight = if (item.income > 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (item.income > 0) Color(0xFF15803D) else Color(0xFF94A3B8),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = if (item.expense > 0) "৳${formatter.format(item.expense.toInt())}" else "0",
                                fontSize = 13.sp,
                                fontWeight = if (item.expense > 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (item.expense > 0) Color(0xFFB91C1C) else Color(0xFF94A3B8),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "৳${formatter.format(item.balance.toInt())}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.balance >= 0) Color(0xFF15803D) else Color(0xFFB91C1C),
                                modifier = Modifier.weight(1.1f),
                                textAlign = TextAlign.End
                            )
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    }
                }

                if (summaryList.size > 7) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isExpanded = !isExpanded },
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpanded) "Show Less 🔺" else "Show All 🔻",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }
                }
            }
        }
    }
}
