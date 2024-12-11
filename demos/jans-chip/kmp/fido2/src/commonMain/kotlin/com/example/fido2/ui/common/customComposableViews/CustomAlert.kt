package com.example.fido2.ui.common.customComposableViews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fido2.Res
import com.example.fido2.cancel
import com.example.fido2.icon_close
import com.example.fido2.ui.theme.LightColors
import com.example.fido2.warning_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun <T> CustomAlert(
    title: String, message: String,
    actionText: String,
    data: T?,
    showAlert: MutableState<Boolean>,
    actionWithValue: ((T) -> Unit)?,
    action: (() -> Unit)?,
) {
    val animationState = remember { MutableTransitionState(false) }

    /// Run only once
    LaunchedEffect(Unit) {
        animationState.targetState = showAlert.value
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End
    ) {

        AnimatedVisibility(
            animationState,
            enter = slideInHorizontally(
                animationSpec = tween(200)
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(200),
                targetOffsetX = { fullWidth -> fullWidth }
            )
        ) {
            Card(
                modifier = Modifier
                    .padding(8.dp),
                shape = RoundedCornerShape(35.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colors.background)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colors.background),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Button(
                                onClick = {
                                    animationState.targetState = false
                                },
                                modifier = Modifier
                                    .height(55.dp),
                                elevation = null,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color.Transparent
                                ),
                            ) {
                                Image(
                                    painterResource(Res.drawable.icon_close),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(30.dp)
                                )
                            }
                        }
                        Image(
                            painterResource(Res.drawable.warning_icon),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .size(60.dp)
                        )
                    }
                    Text(
                        title,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.h5,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = MaterialTheme.colors.onSurface,
                        fontSize = 19.sp,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.subtitle1
                    )

                    /// Buttons
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                animationState.targetState = false

                                if (actionWithValue != null) {
                                    data?.let {
                                        actionWithValue(data)
                                    }
                                } else if (action != null) {
                                    action()
                                }
                            },
                            modifier = Modifier
                                .background(
                                    LightColors.surface,
                                    shape = RoundedCornerShape(35.dp)
                                )
                                .height(55.dp)
                                .weight(1f),
                            elevation = null,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Transparent
                            ),
                        ) {
                            Text(
                                text = actionText,
                                modifier = Modifier,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.h6,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        /// Current state changes after animation finishes
        LaunchedEffect(animationState.currentState) {
            /// Animation is completed when current state is the same as target state.
            /// And currentState is false
            if (!animationState.currentState && animationState.targetState == animationState.currentState) {
                showAlert.value = false
            }
        }
    }
}

@Composable
fun <T> CustomAlertWithTwoButtons(
    title: String, message: String,
    actionText: String,
    data: T?,
    showAlert: MutableState<Boolean>,
    actionWithValue: ((T) -> Unit)?,
    action: (() -> Unit)?,
) {
    val animationState = remember { MutableTransitionState(false) }

    /// Run only once
    LaunchedEffect(Unit) {
        animationState.targetState = showAlert.value
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End
    ) {

        AnimatedVisibility(
            animationState,
            enter = slideInHorizontally(
                animationSpec = tween(200)
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(200),
                targetOffsetX = { fullWidth -> fullWidth }
            )
        ) {
            Card(
                modifier = Modifier
                    .padding(8.dp),
                shape = RoundedCornerShape(35.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colors.background)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colors.background),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Button(
                                onClick = {
                                    animationState.targetState = false
                                },
                                modifier = Modifier
                                    .height(55.dp),
                                elevation = null,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color.Transparent
                                ),
                            ) {
                                Image(
                                    painterResource(Res.drawable.icon_close),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(30.dp)
                                )
                            }
                        }
                    }
                    Text(
                        title,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.h5,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = MaterialTheme.colors.onSurface,
                        fontSize = 19.sp,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.subtitle1
                    )

                    /// Buttons
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                animationState.targetState = false

                                if (actionWithValue != null) {
                                    data?.let {
                                        actionWithValue(data)
                                    }
                                } else if (action != null) {
                                    action()
                                }
                            },
                            modifier = Modifier
                                .background(
                                    LightColors.surface,
                                    shape = RoundedCornerShape(35.dp)
                                )
                                .height(55.dp)
                                .weight(1f),
                            elevation = null,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Transparent
                            ),
                        ) {
                            Text(
                                text = actionText,
                                modifier = Modifier,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.h6,
                                color = Color.White
                            )
                        }
                        //Cancel button
                        Button(
                            onClick = {
                                animationState.targetState = false
                            },
                            modifier = Modifier
                                .background(
                                    LightColors.secondary,
                                    shape = RoundedCornerShape(35.dp)
                                )
                                .height(55.dp)
                                .weight(1f),
                            elevation = null,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Transparent
                            ),
                        ) {
                            Text(
                                text = stringResource(Res.string.cancel),
                                modifier = Modifier,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.h6,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        /// Current state changes after animation finishes
        LaunchedEffect(animationState.currentState) {
            /// Animation is completed when current state is the same as target state.
            /// And currentState is false
            if (!animationState.currentState && animationState.targetState == animationState.currentState) {
                showAlert.value = false
            }
        }
    }
}