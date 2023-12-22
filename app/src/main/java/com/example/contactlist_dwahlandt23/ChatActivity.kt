package com.example.contactlist_dwahlandt23

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contactlist_dwahlandt23.UserCredentials.Companion.userId
import com.example.contactlist_dwahlandt23.ui.theme.ContactList_dwahlandt23Theme
import com.example.contactlist_dwahlandt23.ui.theme.PurpleGrey80
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ContactList_dwahlandt23Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val contactList = MainActivity.contactList
                    val viewModel: ChatViewModel by viewModels()
                    val db = viewModel.connectToDB()
                    val phoneId = Settings.Secure.ANDROID_ID
                    Column(modifier = Modifier.fillMaxSize()) {
                        val contactId: Long = intent.getLongExtra("contactId", 0)
                        val id: Int = contactId.toInt() - 1
                        val contact: Contact = contactList[id]
                        Header(contact)
                        Column() {
                            MessageList(viewModel, db)
                            MyApp(viewModel, db, contactId.toInt(), phoneId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Header(contact: Contact) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.primary)
        .padding(bottom = 8.dp)) {
        Row(modifier = Modifier.padding(start = 16.dp))  {
            Text(
                modifier = Modifier
                    .padding(16.dp)
                    .drawBehind {
                        drawCircle(
                            color = Color.Blue,
                            radius = this.size.maxDimension,
                        )
                    },
                text = contact.name.first().toString(),
                style = TextStyle(color = Color.White, fontSize = 20.sp)
            )
            Row (
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically){
                Text(
                    text = contact.name,
                    modifier = Modifier.padding(16.dp),
                    style = TextStyle(color = Color.White, fontSize = 20.sp)
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp(viewModel: ChatViewModel, db: FirebaseFirestore, chatId: Int, phoneId: String) {
    val chatState = viewModel.chatState
     chatState.db = db
    var msgText by remember { mutableStateOf(chatState.message) }
    var clickedText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    //Set the name depending on the spot in the list

    chatState.chatName = "Chat$chatId"
    Column() {
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {

        }
        Row(modifier = Modifier
            .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextField(
                value = msgText,
                onValueChange = { msgText = it },
                label = { Text("Message")},
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.padding(start = 8.dp))
            Button(onClick = {
                keyboardController?.hide()
                clickedText = msgText
                chatState.message = msgText
                chatState.phoneId = phoneId
                viewModel.onAction(UserAction.SendMessage)
                msgText = ""
            }, modifier = Modifier
                .width(100.dp)
                .padding(end = 8.dp)) {
                Text("Send")
            }
        }
    }
}

@Composable
fun MessageList(viewModel: ChatViewModel, db: FirebaseFirestore) {
    val chatState = viewModel.chatState
    var docSize by remember { mutableIntStateOf(0) }
    // Fetch messages when the composable is (re)composed
    LaunchedEffect(Unit) {
        var doc: Map<String, Map<String, Any>> = viewModel.fetchMessages(viewModel.chatState.chatName, db)
        doc = viewModel.sortMapOnDate(doc)
        chatState.messagesMap = doc
        docSize = doc.size

        Log.d("docSize", "This is the message $docSize")
    }
    Log.d("message", "This is the message")
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(docSize) { index ->
            Log.d("message", "This is the message ${chatState.messagesMap}")
            val uniqueId = chatState.messagesMap?.keys?.elementAt(index)
            val message = chatState.messagesMap?.get(uniqueId)
            if (message != null) {
                chatState.sender = message["senderId"].toString()
                MessageItem(message, chatState)
            }
        }
    }
}

@Composable
fun MessageItem(message: Map<String, Any>, chatState: ChatUiState) {
    // Extract data from the message map
    val text = message["string"].toString()

    if (message["senderId"].toString() == userId) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 48f,
                            topEnd = 48f,
                            bottomStart = 48f,
                            bottomEnd = 0f
                        )
                    )
                    .background(Color.Gray)
                    .padding(16.dp)
            ) {
                Text(text)
            }
            Text(text = message["Date"].toString(), fontSize = 12.sp)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 48f,
                            topEnd = 48f,
                            bottomStart = 0f,
                            bottomEnd = 48f
                        )
                    )
                    .background(Color.Blue)
                    .padding(16.dp)
            ) {
                Text(text)
            }
            Text(text = message["Date"].toString(), fontSize = 12.sp)
        }
    }
}
