package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BazarItemEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

private fun Int.toBanglaDigits(): String {
    val banglaDigits = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )
    return this.toString().map { banglaDigits[it] ?: it }.joinToString("")
}

@Composable
fun BazarScreen(
    bazarItems: List<BazarItemEntity>,
    onRecordSpent: (BazarItemEntity) -> Unit,
    onUpdateSpent: (BazarItemEntity, Double) -> Unit = { _, _ -> },
    onSwapItems: (BazarItemEntity, BazarItemEntity) -> Unit = { _, _ -> },
    onAddToTracker: (title: String, amount: Double, category: String, description: String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var isSortMode by remember { mutableStateOf(false) }
    var isSelectMode by remember { mutableStateOf(false) }
    var disabledItemIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedItemIds by remember { mutableStateOf(setOf<Long>()) }

    // Active (ON) items for Summary Card
    val activeItems = remember(bazarItems, disabledItemIds) {
        bazarItems.filter { it.id !in disabledItemIds }
    }

    // Selected & Active items for Tracker export
    val selectedActiveItems = remember(bazarItems, selectedItemIds, disabledItemIds) {
        bazarItems.filter { it.id in selectedItemIds && it.id !in disabledItemIds }
    }

    val totalTarget = activeItems.sumOf { it.targetPrice }
    val totalSpent = activeItems.sumOf { it.actualSpent }
    val difference = totalTarget - totalSpent

    val totalSelectedAmount = remember(selectedActiveItems) {
        selectedActiveItems.sumOf { if (it.actualSpent > 0) it.actualSpent else it.targetPrice }
    }

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
            contentPadding = PaddingValues(bottom = if (isSelectMode) 80.dp else 16.dp),
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
                if (isSelectMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "সিলেক্ট করা হয়েছে: ${selectedItemIds.size.toBanglaDigits()}টি",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )

                        Text(
                            text = "✕ বাতিল",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isSelectMode = false }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
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
                                onClick = {
                                    isSortMode = !isSortMode
                                },
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

                            Surface(
                                onClick = {
                                    isSelectMode = true
                                    isSortMode = false
                                    selectedItemIds = bazarItems.map { it.id }.toSet()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent
                            ) {
                                Text(
                                    text = "≡ Select",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Items List
            itemsIndexed(bazarItems, key = { index, item -> "${item.id}_$index" }) { index, item ->
                val isOn = item.id !in disabledItemIds
                val isSelected = item.id in selectedItemIds

                BazarCardItem(
                    item = item,
                    isSortMode = isSortMode,
                    isSelectMode = isSelectMode,
                    isSelected = isSelected,
                    isOn = isOn,
                    onToggleSelect = {
                        selectedItemIds = if (isSelected) {
                            selectedItemIds - item.id
                        } else {
                            selectedItemIds + item.id
                        }
                    },
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

        // Floating Bottom Action Bar in Select Mode
        if (isSelectMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // OFF Button
                    Surface(
                        onClick = {
                            disabledItemIds = disabledItemIds + selectedItemIds
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF475569)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✕ অফ (Off)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // ON Button
                    Surface(
                        onClick = {
                            disabledItemIds = disabledItemIds - selectedItemIds
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF10B981)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✓ অন (On)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Add to Tracker Button
                    Surface(
                        onClick = {
                            if (selectedActiveItems.isNotEmpty()) {
                                val desc = selectedActiveItems.joinToString(", ") { "${it.title} (${it.unitQuantity})" }
                                onAddToTracker("বাজার খরচ", totalSelectedAmount, "বাজার", desc)
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF2563EB)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "➤ ট্রেকারে যুক্ত (৳${formatter.format(totalSelectedAmount.toInt())})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BazarCardItem(
    item: BazarItemEntity,
    isSortMode: Boolean,
    isSelectMode: Boolean,
    isSelected: Boolean,
    isOn: Boolean,
    onToggleSelect: () -> Unit,
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

    // Card background: normal white when ON, off-color grey when OFF
    val cardBg = if (isOn) Color.White else Color(0xFFF1F5F9)
    val cardBorderColor = if (isOn) CardBorder else Color(0xFFCBD5E1)
    val titleTextColor = if (isOn) TextPrimary else Color(0xFF64748B)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = isSelectMode) { onToggleSelect() },
        shape = RoundedCornerShape(16.dp),
        color = cardBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Select Mode Checkbox/Radio Circle
            if (isSelectMode) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF15803D) else Color.Transparent)
                        .border(
                            width = if (isSelected) 0.dp else 2.dp,
                            color = if (isSelected) Color.Transparent else Color(0xFFCBD5E1),
                            shape = CircleShape
                        )
                        .clickable { onToggleSelect() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

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

            // Item Title & Target Price
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleTextColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "টার্গেট: ৳${formatter.format(item.targetPrice.toInt())}",
                    fontSize = 12.sp,
                    color = if (isOn) TextMuted else Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Quantity badge
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                color = if (isOn) Color(0xFFEEF2FF) else Color(0xFFE2E8F0)
            ) {
                Text(
                    text = item.unitQuantity,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOn) Color(0xFF4F46E5) else Color(0xFF64748B),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Price Field
            if (!isOn) {
                // Disabled when OFF
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.height(36.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OFF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else if (isEditing) {
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
                                    text = "খরচ লিখুন",
                                    fontSize = 11.sp,
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
                    onClick = { if (!isSelectMode) isEditing = true },
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
                            text = if (item.actualSpent > 0) "৳${formatter.format(item.actualSpent.toInt())}" else "খরচ লিখুন",
                            fontSize = 11.sp,
                            fontWeight = if (item.actualSpent > 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (item.actualSpent > 0) Color(0xFF15803D) else TextMuted
                        )
                    }
                }
            }
        }
    }
}
