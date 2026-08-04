package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PeopleDueEntity
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AccountScreen(
    dues: List<PeopleDueEntity>,
    onEditDue: (PeopleDueEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPayable = dues.sumOf { it.amountOwed }
    val totalReceivable = dues.sumOf { it.amountReceivable }
    val netStatus = totalReceivable - totalPayable

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
                            text = "নিট স্ট্যাটাস (Net Status)",
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
                                imageVector = Icons.Default.Person,
                                contentDescription = "User",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (netStatus < 0) "-৳ ${formatter.format(-netStatus.toInt())}" else "৳ ${formatter.format(netStatus.toInt())}",
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
                                text = "পাওনা (Receivable)",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳ ${formatter.format(totalReceivable.toInt())}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "দেনা (Payable)",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳ ${formatter.format(totalPayable.toInt())}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Ratio Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পাওনা অনুপাত",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "0%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
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
            DueCard(
                due = due,
                onEdit = { onEditDue(due) }
            )
        }
    }
}

@Composable
private fun DueCard(
    due: PeopleDueEntity,
    onEdit: () -> Unit
) {
    val formatter = NumberFormat.getNumberInstance(Locale.US)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initial circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(RedIconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = due.initial,
                    color = RedIconColor,
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
                    text = "Net: You owe ৳${formatter.format(due.amountOwed.toInt())}",
                    fontSize = 13.sp,
                    color = RedAccent,
                    fontWeight = FontWeight.Medium
                )
            }

            // Edit button
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
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
