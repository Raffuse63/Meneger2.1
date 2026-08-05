package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BazarItemEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BazarScreen(
    bazarItems: List<BazarItemEntity>,
    onRecordSpent: (BazarItemEntity) -> Unit,
    onUpdateSpent: (BazarItemEntity, Double) -> Unit = { _, _ -> },
    onSwapItems: (BazarItemEntity, BazarItemEntity) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val totalTarget = bazarItems.sumOf { it.targetPrice }
    val totalSpent = bazarItems.sumOf { it.actualSpent }
    val difference = totalTarget - totalSpent

    val progressVal = remember(totalSpent, totalTarget) {
        if (totalTarget > 0 && !totalSpent.isNaN() && !totalTarget.isNaN()) {
            val ratio = (totalSpent / totalTarget).toFloat()
            if (ratio.isNaN()) 0f else ratio.coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    val formatter = NumberFormat.getNumberInstance(Locale.US)
    val focusManager = LocalFocusManager.current

    var isSortMode by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary Card
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
                                text = "Actual Bazar Spend",
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
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Bazar",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "৳ ${formatter.format(totalSpent.toInt())}",
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
                                    text = "Target Budget",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "৳ ${formatter.format(totalTarget.toInt())}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Difference",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "+৳ ${formatter.format(difference.toInt())}",
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
                                text = "Bazar Budget Used",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                            val pct = if (totalTarget > 0) ((totalSpent / totalTarget) * 100).toInt() else 0
                            Text(
                                text = "$pct%",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { progressVal },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = GreenAccent,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // Subheader
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bazar List",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { isSortMode = !isSortMode },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSortMode) Color(0xFFDCFCE7) else Color.Transparent,
                            border = if (isSortMode) BorderStroke(1.dp, Color(0xFF15803D)) else null
                        ) {
                            Text(
                                text = if (isSortMode) "✓ Done" else "⇅ Sort",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "≡ Select",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }

            // Items
            itemsIndexed(bazarItems, key = { index, item -> "${item.id}_$index" }) { index, item ->
                BazarCardItem(
                    item = item,
                    isSortMode = isSortMode,
                    isFirst = index == 0,
                    isLast = index == bazarItems.size - 1,
                    onMoveUp = {
                        if (index > 0 && index < bazarItems.size) {
                            onSwapItems(bazarItems[index], bazarItems[index - 1])
                        }
                    },
                    onMoveDown = {
                        if (index >= 0 && index < bazarItems.size - 1) {
                            onSwapItems(bazarItems[index], bazarItems[index + 1])
                        }
                    },
                    onRecordSpent = { onRecordSpent(item) },
                    onUpdateSpent = { newSpent -> onUpdateSpent(item, newSpent) }
                )
            }
        }
    }
}

@Composable
private fun BazarCardItem(
    item: BazarItemEntity,
    isSortMode: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRecordSpent: () -> Unit,
    onUpdateSpent: (Double) -> Unit
) {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    val focusManager = LocalFocusManager.current

    var isEditing by remember { mutableStateOf(false) }
    var hasGainedFocus by remember { mutableStateOf(false) }

    var tempPriceValue by remember(item.actualSpent, isEditing) {
        val text = if (item.actualSpent > 0) item.actualSpent.toInt().toString() else ""
        mutableStateOf(
            TextFieldValue(
                text = text,
                selection = TextRange(text.length)
            )
        )
    }

    val focusRequester = remember { FocusRequester() }

    val saveAndClose = {
        if (isEditing) {
            val parsed = tempPriceValue.text.toDoubleOrNull() ?: 0.0
            if (parsed != item.actualSpent) {
                onUpdateSpent(parsed)
            }
            isEditing = false
            hasGainedFocus = false
        }
    }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            hasGainedFocus = false
            delay(50)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sort controls (Up / Down arrows)
            if (isSortMode) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = if (!isFirst) Color(0xFF15803D) else Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isLast,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = if (!isLast) Color(0xFF15803D) else Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Target: ৳${formatter.format(item.targetPrice.toInt())}",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Quantity badge
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                color = Color(0xFFEEF2FF)
            ) {
                Text(
                    text = item.unitQuantity,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4F46E5),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Price Field (Inline Editable / Clickable Text)
            if (isEditing) {
                BasicTextField(
                    value = tempPriceValue,
                    onValueChange = { tempPriceValue = it },
                    modifier = Modifier
                        .width(72.dp)
                        .height(36.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                hasGainedFocus = true
                            } else if (hasGainedFocus && !focusState.isFocused && isEditing) {
                                saveAndClose()
                            }
                        },
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            saveAndClose()
                            focusManager.clearFocus()
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF0FDF4), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF15803D), RoundedCornerShape(10.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (tempPriceValue.text.isEmpty()) {
                                Text(
                                    text = "Prize",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            } else {
                Surface(
                    onClick = { isEditing = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (item.actualSpent > 0) Color(0xFF15803D) else CardBorder),
                    color = if (item.actualSpent > 0) Color(0xFFDCFCE7) else Color.White,
                    modifier = Modifier.height(36.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (item.actualSpent > 0) "৳${formatter.format(item.actualSpent.toInt())}" else "Prize",
                            fontSize = 12.sp,
                            fontWeight = if (item.actualSpent > 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (item.actualSpent > 0) Color(0xFF15803D) else TextSecondary
                        )
                    }
                }
            }
        }
    }
}


