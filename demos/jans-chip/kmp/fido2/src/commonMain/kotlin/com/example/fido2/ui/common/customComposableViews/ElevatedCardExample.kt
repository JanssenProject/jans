package com.example.fido2.ui.common.customComposableViews

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fido2.Res
import com.example.fido2.continue_
import com.example.fido2.ui.theme.AppTheme
import com.example.fido2.ui.theme.LightColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ElevatedCardExample(
    heading: String,
    subheading: String,
    icon: DrawableResource,
    onButtonClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(
                LightColors.background,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .padding(AppTheme.dimens.paddingNormal)
        ) {// 1
            Row {
                Image(
                    painterResource(icon),
                    contentDescription = "",
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.size(64.dp)
                )
                Column {
                    Text(
                        text = heading,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        ),
                        modifier = Modifier
                            .padding(1.dp),
                    )
                    Text(
                        text = subheading,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                        ),
                        modifier = Modifier
                            .padding(1.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.End) {
                LoginButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.continue_),
                    onClick = onButtonClick
                )
            }
        }
    }
}