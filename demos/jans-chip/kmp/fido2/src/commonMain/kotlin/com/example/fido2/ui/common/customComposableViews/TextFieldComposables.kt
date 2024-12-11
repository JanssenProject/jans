package com.example.fido2.ui.common.customComposableViews

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Email Text Field
 */
@Composable
fun CustomTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    errorText: String = "",
    imeAction: ImeAction = ImeAction.Next
) {
    Text(modifier = Modifier
        .padding(vertical = 0.dp),
        text = label,
        color = Color.Black)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        shape = RoundedCornerShape(32.dp),
        modifier = modifier,
        textStyle = TextStyle(color = Color.Black),
        label = {
            Text(text = "", color = Color.Black)
        },
        maxLines = 1,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = imeAction
        ),
        isError = isError
    )
    if (isError) {
        ErrorTextInputField(text = errorText)
    }
}