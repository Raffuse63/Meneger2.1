package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PeopleDueEntity
import com.example.data.PersonTransactionEntity
import com.example.data.TransactionPaymentEntity
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AccountScreen(
    dues: List<PeopleDueEntity>,
    transactions: List<PersonTransactionEntity> = emptyList(),
    payments: List<TransactionPaymentEntity> = emptyList(),
    onEditDue: (PeopleDueEntity) -> Unit,
    onDeleteDue: (PeopleDueEntity) -> Unit = {},
    onAddTransaction: (PeopleDueEntity) -> Unit = {},
    onAddPayment: (PersonTransactionEntity, remainingAmount: Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var expandedPersonId by remember { mutableStateOf<Long?>(null) }

    val totalPayable = dues.sumOf { it.amountOwed }
    val totalReceivable = dues.sumOf { it.amountReceivable }
    val netStatus = totalReceivable - totalPayable

    val totalSum = totalReceivable + totalPayable
    val ratio = if (totalSum > 0) ((totalReceivable / totalSum) * 100).toInt() else 0
    val progress = if (totalSum > 0) (totalReceivable / totalSum).toFloat() else 0f

    val formatter = NumberFormat.getNumberInstance(Locale.US)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Net Status Summary Card
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
                            text = "Net Status",
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
                                imageVector = Icons.Default.Person,
                                contentDescription = "User",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (netStatus < 0) "-৳ ${formatter.format(-netStatus.toInt())}" else "৳ ${formatter.format(netStatus.toInt())}",
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
                                text = "Receivable",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "৳ ${formatter.format(totalReceivable.toInt())}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Payable",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "৳ ${formatter.format(totalPayable.toInt())}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Ratio Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Receivable Ratio",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$ratio%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }

        // Section header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(text = "📋", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "People Dues List",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        items(dues, key = { it.id }) { due ->
            val isExpanded = expandedPersonId == due.id
            val personTxList = transactions.filter { it.personId == due.id }

            DueCard(
                due = due,
                isExpanded = isExpanded,
                personTransactions = personTxList,
                allPayments = payments,
                onToggleExpand = {
                    expandedPersonId = if (isExpanded) null else due.id
                },
                onEdit = { onEditDue(due) },
                onLongPress = { onDeleteDue(due) },
                onAddTransaction = { onAddTransaction(due) },
                onAddPayment = onAddPayment
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DueCard(
    due: PeopleDueEntity,
    isExpanded: Boolean,
    personTransactions: List<PersonTransactionEntity>,
    allPayments: List<TransactionPaymentEntity>,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onLongPress: () -> Unit,
    onAddTransaction: () -> Unit,
    onAddPayment: (PersonTransactionEntity, Double) -> Unit
) {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    val netBalance = due.amountReceivable - due.amountOwed

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Main clickable & long-pressable row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onToggleExpand() },
                        onLongClick = { onLongPress() }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Initial circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                netBalance > 0 -> Color(0xFFDCFCE7)
                                netBalance < 0 -> RedIconBg
                                else -> Color(0xFFF1F5F9)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = due.initial,
                        color = when {
                            netBalance > 0 -> Color(0xFF15803D)
                            netBalance < 0 -> RedIconColor
                            else -> TextSecondary
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Text
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = due.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            netBalance > 0 -> "Receivable: +৳${formatter.format(netBalance.toInt())}"
                            netBalance < 0 -> "Payable: -৳${formatter.format((-netBalance).toInt())}"
                            else -> "৳0 (Balanced)"
                        },
                        fontSize = 13.sp,
                        color = when {
                            netBalance > 0 -> Color(0xFF16A34A)
                            netBalance < 0 -> RedAccent
                            else -> TextMuted
                        },
                        fontWeight = FontWeight.Medium
                    )
                }

                // Edit button (Edits person name only)
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Expand indicator
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand details",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Expanded detail section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Divider(color = Color(0xFFE2E8F0))

                    // + Add transition button
                    Button(
                        onClick = onAddTransaction,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Transaction",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ Add Transaction",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Transaction history header
                    Text(
                        text = "Transaction History:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (personTransactions.isEmpty()) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No transactions found",
                                fontSize = 13.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        personTransactions.forEach { tx ->
                            val txPayments = allPayments.filter { it.transactionId == tx.id }
                            val totalPaid = txPayments.sumOf { it.paidAmount }
                            val remaining = (tx.amount - totalPaid).coerceAtLeast(0.0)

                            TransactionHistoryCard(
                                transaction = tx,
                                payments = txPayments,
                                remainingAmount = remaining,
                                onPayClick = { onAddPayment(tx, remaining) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionHistoryCard(
    transaction: PersonTransactionEntity,
    payments: List<TransactionPaymentEntity>,
    remainingAmount: Double,
    onPayClick: () -> Unit
) {
    val formatter = NumberFormat.getNumberInstance(Locale.US)

    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Type Badge & Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (transaction.isGive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (transaction.isGive) "I give (Receivable +)" else "I receive (Payable -)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.isGive) Color(0xFF15803D) else Color(0xFFB91C1C),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "৳${formatter.format(transaction.amount.toInt())}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.isGive) Color(0xFF16A34A) else RedAccent
                )
            }

            // Details and date-time
            Column {
                Text(
                    text = transaction.details,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.dateTime,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            // Payment history list
            if (payments.isNotEmpty()) {
                Divider(color = Color(0xFFCBD5E1), thickness = 0.5.dp)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    payments.forEach { pm ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Pay",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = pm.dateTime,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = "৳${formatter.format(pm.paidAmount.toInt())}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenAccent
                            )
                        }
                    }
                }
            }

            Divider(color = Color(0xFFCBD5E1), thickness = 0.5.dp)

            // Bottom action & Remaining row: [Pay] button | Remaining: amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPayClick,
                    enabled = remainingAmount > 0,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenAccent,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (remainingAmount > 0) "Pay" else "Fully Paid",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Remaining: ৳${formatter.format(remainingAmount.toInt())}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (remainingAmount == 0.0) GreenAccent else TextPrimary
                )
            }
        }
    }
}


