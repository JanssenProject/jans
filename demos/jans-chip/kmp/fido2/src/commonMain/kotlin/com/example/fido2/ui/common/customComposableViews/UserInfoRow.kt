package com.example.fido2.ui.common.customComposableViews

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fido2.ui.theme.LightColors

@Composable
fun UserInfoRow(
    label: String,
    value: String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(15.dp),
        backgroundColor = LightColors.background
    ) {
        Row {  // 1
            Text(
                text = "$label:",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                modifier = Modifier
                    .padding(5.dp),
            )
            Text(
                text = value,
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontSize = 20.sp,

                    ),
                modifier = Modifier
                    .padding(5.dp),
            )
        }
    }
}