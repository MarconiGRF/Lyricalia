package com.example.lyricalia

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lyricalia.ui.theme.LyricaliaTheme

class MainActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LyricaliaTheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF5A5766)),
                    contentAlignment = Alignment.Center
                ) {
                    SignupField()
                }
            }
        }
    }
}

@Composable
fun SignupField() {
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.background(Color(0xFF5A5766)),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            username,
            onValueChange = { username = it },
            label = { Text("Username") },
            colors = TextFieldDefaults.colors(
                focusedPlaceholderColor = Color(0xFF48435C),
                focusedLabelColor = Color(0xFFEDFFEC),
                focusedTextColor = Color(0xFFEDFFEC),
                unfocusedPlaceholderColor = Color(0xFF48435C),
                unfocusedLabelColor = Color(0xFF48435C),
                unfocusedTextColor = Color(0xFF48435C)
            )
        )

        OutlinedTextField(
            name,
            onValueChange = { name = it },
            label = { Text("name") },
            colors = TextFieldDefaults.colors(
                focusedPlaceholderColor = Color(0xFF48435C),
                focusedLabelColor = Color(0xFFEDFFEC),
                focusedTextColor = Color(0xFFEDFFEC),
                unfocusedPlaceholderColor = Color(0xFF48435C),
                unfocusedLabelColor = Color(0xFF48435C),
                unfocusedTextColor = Color(0xFF48435C)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LyricaliaTheme {
        SignupField()
    }
}