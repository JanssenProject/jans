package com.example.rust_android

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidproj.ui.theme.AndroidProjTheme
import uniffi.mobile.Cedarling
import java.io.IOException


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AndroidProjTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        authResult = "Testing Cedarling",
                        modifier = Modifier.padding(innerPadding)
                    )
                    MyButton()
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, authResult: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name! , Result is $authResult .",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidProjTheme {
        Greeting("Android", "")
    }
}


@Composable
fun MyButton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val context = LocalContext.current

        Button(
            onClick = {
                Toast.makeText(context, "Welcome to Geeks for Geeks", Toast.LENGTH_LONG).show()
                //Read bootstrap json
                val bootstrapConfig = readJsonFromAssets(context, "bootstrap.json")
                //Create cedarling instance
                var instance: Cedarling? = bootstrapConfig?.let { Cedarling.loadFromJson(it) };
                //Collect data for authorization request
                val access_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJhY2Nlc3NfdGtuX2p0aSIsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDEsInVyaSI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZy9qYW5zLWF1dGgvcmVzdHYxL3N0YXR1c19saXN0In19fQ.D6q28qP-rZ3LayPsVlvUzXCwHtl7g3VTntMQvG_f3mM";
                val id_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsImFtciI6InB3ZCIsInVzZXJuYW1lIjoiYWRtaW5AZ2x1dS5vcmciLCJjb3VudHJ5IjoidXNhIiwieDV0I1MyNTYiOiIiLCJzY29wZSI6WyJvcGVuaWQiLCJwcm9maWxlIl0sIm9yZ19pZCI6InNvbWVfbG9uZ19pZCIsImF1dGhfdGltZSI6MTcyNDgzMDc0NiwiZXhwIjoxNzI0OTQ1OTc4LCJpYXQiOjE3MjQ4MzIyNTksImp0aSI6ImlkX3Rrbl9qdGkiLCJuYW1lIjoiRGVmYXVsdCBBZG1pbiBVc2VyIiwic3RhdHVzIjp7InN0YXR1c19saXN0Ijp7ImlkeCI6MjAxLCJ1cmkiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmcvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.xVRNRN7RW3Y2n4bzW0k93zbe5Tn0htQS6JiVq9NP0NE";
                val userinfo_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY2xpZW50X2lkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwiYXVkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwidXNlcm5hbWUiOiJhZG1pbkBnbHV1Lm9yZyIsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJlbWFpbCI6ImFkbWluQGdsdXUub3JnIiwiY291bnRyeSI6IlVTIiwianRpIjoidXNyaW5mb190a25fanRpIn0.NoR53vPZFpfb4vFk85JH9RPx7CHsaJMZwrH3fnB-N60";
                val action = "Jans::Action::\"Update\"";
                val context = """
            {}
        """.trimIndent();

                val resource_type = "Jans::Issue";
                val resource_id = "some_id";
                val payload = """
            {
            "org_id": "some_long_id",
            "country": "US"
        }
        """.trimIndent();
                //Call authorize method
                val result = instance?.authorize(access_token,
                    id_token,
                    userinfo_token,
                    action,
                    resource_type,
                    resource_id,
                    payload,
                    context
                )
                

            },
            modifier = Modifier.padding(16.dp),
            enabled = true,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color.Green
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
            border = BorderStroke(width = 2.dp, brush = SolidColor(Color.Blue)),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 12.dp,
                end = 20.dp,
                bottom = 12.dp
            ),
            interactionSource = remember { MutableInteractionSource() }
        ) {
            Text(
                text = "Geeks for Geeks",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif
            )
        }
    }
}

fun readJsonFromAssets(context: Context, fileName: String): String? {
    return try {
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (ex: IOException) {
        ex.printStackTrace()
        null
    }
}
