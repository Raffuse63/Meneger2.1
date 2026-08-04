package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BudgetExpenseEntity
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BudgetScreen(
    expenses: List<BudgetExpenseEntity>,
    onDeleteExpense: (BudgetExpenseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalBudget = 25000.0
    val totalExpense = expenses.sumOf { it.amount }
    val remainingBalance = totalBudget - totalExpense

    val formatter = NumberFormat.getNumberInstance(Locale.US)

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
                            text = "মোট বাজেট",
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
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = "Budget",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "৳ ${formatter.format(totalBudget.toInt())}",
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
                                text = "মোট খরচ",
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

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "অবশিষ্ট ব্যালেন্স",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳ ${formatter.format(remainingBalance.toInt())}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val usedPercent = ((totalExpense / totalBudget) * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ব্যবহৃত হয়েছে",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$usedPercent%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (totalExpense / totalBudget).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF38BDF8),
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }

        // Section header
        item {
            Text(
                text = "সাম্প্রতিক খরচ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(expenses, key = { it.id }) { expense ->
            BudgetExpenseCard(expense = expense)
        }
    }
}

@Composable
private fun BudgetExpenseCard(
    expense: BudgetExpenseEntity
) {
    val formatter = NumberFormat.getNumberInstance(Locale.US)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Receipt Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDBEAFE)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🧾", fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = expense.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)),
                        color = Color(0xFFEFF6FF)
                    ) {
                        Text(
                            text = expense.tag,
                            fontSize = 12.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "• ${expense.dateText}", fontSize = 12.sp, color = TextMuted)
                }
            }

            Text(
                text = "- ৳ ${formatter.format(expense.amount.toInt())}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = RedAccent
            )
        }
    }
}
