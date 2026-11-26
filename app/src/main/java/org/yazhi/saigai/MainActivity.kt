package org.yazhi.saigai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.yazhi.saigai.ui.SaigaiApp
import org.yazhi.saigai.ui.theme.SaigaiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SaigaiTheme {
                SaigaiApp()
            }
        }
    }
}
