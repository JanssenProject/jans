package com.example.fido2.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.example.fido2.Res
import com.example.fido2.compose_multiplatform
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

@Composable
fun SplashScreen(
    viewModel: SplashScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel { SplashScreenViewModel() },
    onProceedToNextScreen: () -> Unit,
) {
    val scale = remember {
        Animatable(0F)
    }

    LaunchedEffect(true) {
        scale.animateTo(
            targetValue = 0.7F,
            animationSpec = tween(
                durationMillis = 500
            )
        )
        delay(1000L)
        onProceedToNextScreen()
    }


    Column(
        modifier = Modifier.background(Color(0xFF7C4DFF)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(Res.drawable.compose_multiplatform),
            contentDescription = null,
            modifier = Modifier.scale(scale.value)
        )
    }
}