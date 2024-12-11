package com.example.fido2.ui.common.customComposableViews

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fido2.ui.theme.AppTheme
import com.example.fido2.ui.theme.LightColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class Direction {
    LEFT, RIGHT
}

@Composable
fun NormalButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier
            .height(AppTheme.dimens.normalButtonHeight),
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
        onClick = onClick
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle2,
            color = Color.White)
    }
}

@Composable
fun LoginButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier
            .height(AppTheme.dimens.normalButtonHeight),
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = LightColors.surface),
        onClick = onClick
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle2,
            color = Color.White)
    }
}

@Composable
fun SmallClickableWithIconAndText(
    modifier: Modifier = Modifier,
    icon: DrawableResource,
    text: String = "",
    direction: Direction = Direction.LEFT,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (direction == Direction.LEFT) {Image(
            painterResource(icon),
            contentDescription = null,
            modifier = Modifier.clickable {
                onClick.invoke()
            }
                .size(25.dp)
                .padding(start = 12.dp)
        )
            Text(
                modifier = Modifier.clickable {
                    onClick.invoke()
                }
                    .padding(start = AppTheme.dimens.paddingSmall),
                text = text,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.primary
            )
        } else {
            Text(
                modifier = Modifier.clickable {
                    onClick.invoke()
                }
                    .padding(start = AppTheme.dimens.paddingSmall),
                text = text,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.primary
            )
            Image(
                painterResource(icon),
                contentDescription = null,
                modifier = Modifier.clickable {
                    onClick.invoke()
                }
                    .size(25.dp)
                    .padding(start = 12.dp)
            )
        }
    }
}