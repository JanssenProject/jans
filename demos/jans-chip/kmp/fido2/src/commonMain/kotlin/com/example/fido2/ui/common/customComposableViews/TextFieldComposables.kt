package com.example.fido2.ui.common.customComposableViews

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.fido2.*
import org.jetbrains.compose.resources.stringResource

/**
 * Password Text Field
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
fun PasswordTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    errorText: String = "",
    imeAction: ImeAction = ImeAction.Done
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    var isPasswordVisible by remember {
        mutableStateOf(false)
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = TextStyle(color = Color.Black),
        label = {
            Text(text = label, color = Color.Black)
        },
        trailingIcon = {
            IconButton(onClick = {
                isPasswordVisible = !isPasswordVisible
            }) {

                val visibleIconAndText = Pair(
                    first = Icons.Outlined.Add,
                    second = stringResource(Res.string.icon_password_visible)
                )

                val hiddenIconAndText = Pair(
                    first = Icons.Outlined.Done,
                    second = stringResource(Res.string.icon_password_hidden)
                )

                val passwordVisibilityIconAndText =
                    if (isPasswordVisible) visibleIconAndText else hiddenIconAndText

                // Render Icon
                Icon(
                    imageVector = passwordVisibilityIconAndText.first,
                    contentDescription = passwordVisibilityIconAndText.second
                )
            }
        },
        singleLine = true,
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = {
            keyboardController?.hide()
        }),
        isError = isError,
        /*supportingText = {
            if (isError) {
                ErrorTextInputField(text = errorText)
            }
        }*/
    )
}

/**
 * Email Text Field
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EmailTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    errorText: String = "",
    imeAction: ImeAction = ImeAction.Next
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = TextStyle(color = Color.Black),
        label = {
            Text(text = label, color = Color.Black)
        },
        maxLines = 1,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = imeAction
        ),
        isError = isError,
//        supportingText = {
//            if (isError) {
//                ErrorTextInputField(text = errorText)
//            }
//        }
    )

}

/**
 * Mobile Number Text Field
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MobileNumberTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    errorText: String = "",
    imeAction: ImeAction = ImeAction.Next
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = {
            Text(text = label)
        },
        maxLines = 1,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = imeAction
        ),
        isError = isError
//        supportingText = {
//            if (isError) {
//                ErrorTextInputField(text = errorText)
//            }
//        }
    )

}