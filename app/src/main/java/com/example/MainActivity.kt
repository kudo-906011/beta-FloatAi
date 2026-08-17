package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.data.OverlayStateManager
import com.example.ui.navigation.ReplyFloatApp
import com.example.ui.theme.ReplyFloatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        OverlayStateManager.refreshPermissions(this)
        setContent {
            ReplyFloatTheme {
                ReplyFloatApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        OverlayStateManager.refreshPermissions(this)
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    ReplyFloatTheme {
        ReplyFloatApp()
    }
}
