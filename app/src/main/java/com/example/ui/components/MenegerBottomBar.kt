package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppTab
import com.example.ui.theme.HeaderBlue

@Composable
fun MenegerBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(HeaderBlue)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            label = "Notebook",
            icon = { Text("📋", fontSize = 18.sp) },
            isSelected = selectedTab == AppTab.NOTEBOOK,
            onClick = { onTabSelected(AppTab.NOTEBOOK) }
        )
        BottomNavItem(
            label = "Account",
            icon = { Icon(Icons.Default.Person, contentDescription = "Account", tint = Color.White, modifier = Modifier.size(22.dp)) },
            isSelected = selectedTab == AppTab.ACCOUNT,
            onClick = { onTabSelected(AppTab.ACCOUNT) }
        )
        BottomNavItem(
            label = "Tracker",
            icon = { Text("💸", fontSize = 18.sp) },
            isSelected = selectedTab == AppTab.TRACKER,
            onClick = { onTabSelected(AppTab.TRACKER) }
        )
        BottomNavItem(
            label = "Bazar",
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Bazar", tint = Color.White, modifier = Modifier.size(22.dp)) },
            isSelected = selectedTab == AppTab.BAZAR,
            onClick = { onTabSelected(AppTab.BAZAR) }
        )
        BottomNavItem(
            label = "Budget",
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Budget", tint = Color.White, modifier = Modifier.size(22.dp)) },
            isSelected = selectedTab == AppTab.BUDGET,
            onClick = { onTabSelected(AppTab.BUDGET) }
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundModifier = if (isSelected) {
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.25f))
    } else {
        Modifier
    }

    Column(
        modifier = backgroundModifier
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
