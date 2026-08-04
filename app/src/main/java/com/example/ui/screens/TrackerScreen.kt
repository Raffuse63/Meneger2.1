package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TrackerRecordEntity
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TrackerScreen(
    records: List<TrackerRecordEntity>,
    onEditRecord: (TrackerRecordEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalExpense = records.filter { it.isExpense }.sumOf { it.amount }
    val totalIncome = records.filter { !it.isExpense }.sumOf { it.amount }
    val previousMonthBalance = 2050.0
    val currentBalance = previousMonthBalance + totalIncome - totalExpense

    val formatter = NumberFormat.getNumberInstance(Locale.US)

    var selectedYear by remember { mutableStateOf("2026") }
    var selectedMonth by remember { mutableStateOf("August") }
    var selectedDay by remember { mutableStateOf("All Days") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SummaryCardBlue)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "মাসিক ব্যালেন্স (Balance)",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "💸", fontSize = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "৳ ${formatter.format(currentBalance.toInt())}",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "মোট আয় (Income)",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳ ${formatter.format(totalIncome.toInt())}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "মোট খরচ (Expense)",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳ ${formatter.format(totalExpense.toInt())}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পূর্বের মাস: ৳ ${formatter.format(previousMonthBalance.toInt())}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "100%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFF87171),
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }

        // Filter Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔍", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Filter & Search Records",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterPill(text = "$selectedYear ▼", modifier = Modifier.weight(1f))
                        FilterPill(text = "$selectedMonth ▼", modifier = Modifier.weight(1.2f))
                        FilterPill(text = "$selectedDay ▼", modifier = Modifier.weight(1.3f))
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🔍", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "Search", fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Records List Title
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(text = "📋", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Records List",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        // Records items
        items(records, key = { it.id }) { record ->
            TrackerRecordItem(record = record, onEdit = { onEditRecord(record) })
        }

        // Daily Summary
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📅", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Daily Summary",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Date",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = "Income",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenAccent,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Expense",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedAccent,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Balance",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    // Table Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1 August 26",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = "0",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "৳${formatter.format(totalExpense.toInt())}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedAccent,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "৳${formatter.format(currentBalance.toInt())}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerRecordItem(
    record: TrackerRecordEntity,
    onEdit: () -> Unit
) {
    val formatter = NumberFormat.getNumberInstance(Locale.US)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left red strip
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(64.dp)
                    .background(RedAccent, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${record.dateString} • 👤 ${record.category}",
                        fontSize = 12.sp,
                        color = PrimaryBlue
                    )
                }

                Text(
                    text = "৳${formatter.format(record.amount.toInt())}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedAccent
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        color = Color(0xFFF1F5F9)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}
