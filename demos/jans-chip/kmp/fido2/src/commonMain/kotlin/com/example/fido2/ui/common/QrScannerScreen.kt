package com.example.fido2.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import qrscanner.QrScanner
import utils.getPlatform

@Composable
fun QrScannerView(onNavigateBack: () -> Unit) {
    var qrCodeURL by remember { mutableStateOf("") }
    var flashlightOn by remember { mutableStateOf(false) }
    var openImagePicker by remember { mutableStateOf(value = false) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier
                .background(Color(0xFF1D1C22))
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = if (getPlatform().name != "Desktop") {
                    Modifier
                        .size(200.dp)
                        .clip(shape = RoundedCornerShape(size = 14.dp))
                        .clipToBounds()
                        .border(2.dp, Color.Gray, RoundedCornerShape(size = 14.dp))
                } else {
                    Modifier
                },
                contentAlignment = Alignment.Center
            ) {
                QrScanner(
                    modifier = Modifier
                        .clipToBounds()
                        .clip(shape = RoundedCornerShape(size = 14.dp)),
                    flashlightOn = flashlightOn,
                    openImagePicker = openImagePicker,
                    onCompletion = {
                        qrCodeURL = it
                    },
                    imagePickerHandler = {
                        openImagePicker = it
                    },
                    onFailure = {
                        coroutineScope.launch {
                            if (it.isEmpty()) {
                                println("Invalid qr code")
                            } else {
                                println(it)
                            }
                        }
                    }
                )
            }

            if (getPlatform().name != "Desktop") {
                Box(
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                        .background(
                            color = Color(0xFFF9F9F9),
                            shape = RoundedCornerShape(25.dp)
                        )
                        .height(35.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 4.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (flashlightOn) Icons.Filled.Call else Icons.Filled.Add,
                            "flash",
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    flashlightOn = !flashlightOn
                                }
                        )

//                        VerticalDivider(
//                            modifier = Modifier,
//                            thickness = 1.dp,
//                            color = Color(0xFFD8D8D8)
//                        )

//                        Image(
//                            painter = painterResource(Res.drawable.),
//                            contentDescription = "gallery",
//                            contentScale = ContentScale.Fit,
//                            modifier = Modifier
//                                .size(20.dp)
//                                .clickable {
//                                    openImagePicker = true
//                                }
//                        )
                    }
                }
            } else {
                Button(
                    modifier = Modifier.padding(top = 12.dp),
                    onClick = {
                        openImagePicker = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color(0xFF5144D8)
                    ),
                ) {
                    Text(
                        text = "Select Image",
                        modifier = Modifier.background(Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Serif
                    )
                }
            }
        }

        if (qrCodeURL.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 22.dp)
                    .fillMaxWidth()
                    .align(
                        Alignment.BottomCenter
                    ), verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = qrCodeURL,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .weight(1f),
                    fontSize = 12.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Icon(
                    Icons.Filled.Add,
                    "CopyAll",
                    modifier = Modifier.size(20.dp).clickable {
                        clipboardManager.setText(AnnotatedString((qrCodeURL)))
                    },
                    tint = Color.White
                )
            }
        }

        Row(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QRScanner",
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )

            Icon(
                Icons.Filled.Close,
                "close",
                modifier = Modifier.size(20.dp).clickable {
                    onNavigateBack.invoke()
                },
                tint = Color.White
            )
        }
    }
}