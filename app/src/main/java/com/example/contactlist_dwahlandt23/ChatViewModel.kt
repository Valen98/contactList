package com.example.contactlist_dwahlandt23

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.CompletableFuture

class ChatViewModel(): ViewModel() {

    var chatState by mutableStateOf(ChatUiState())
    fun connectToDB(): FirebaseFirestore {
        return Firebase.firestore
    }

    fun onAction(action: UserAction) {
        when(action) {
            is UserAction.SendMessage -> {
                chatState.db?.let { sendMessage(chatState.chatName, chatState.message, it) }

            }
        }
    }

    suspend fun fetchMessages(chatRoomName: String, db: FirebaseFirestore): Map<String, Map<String, Any>> {
        val completableFuture = CompletableFuture<Map<String, Map<String, Any>>>()

        val docRef = db.collection(chatRoomName)
        docRef.get()
            .addOnSuccessListener { querySnapshot ->
                val messagesMap = mutableMapOf<String, Map<String, Any>>()

                for (document in querySnapshot.documents) {
                    val messageId = document.id
                    val messageData = document.data
                    messagesMap[messageId] = messageData ?: emptyMap()
                }

                Log.d("fetchMSG", "Messages data: $messagesMap ")
                completableFuture.complete(messagesMap)
            }
            .addOnFailureListener { exception ->
                Log.d("fetchMSG", "get failed with", exception)
                completableFuture.completeExceptionally(exception)
            }

        return completableFuture.await()
    }

    private fun sendMessage(chatRoomName: String, msg: String, db: FirebaseFirestore) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val current = LocalDateTime.now().format(formatter)
        val data = mapOf(
            "senderId" to UserCredentials.userId,
            "senderName" to UserCredentials.username,
            "string" to msg,
            "Date" to current
        )

        db.collection(chatRoomName)
            .add(data)
            .addOnSuccessListener { documentReference ->
                Log.d("BIGTAG", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.d("BIGTAG", "Error adding Document: $e")
            }



        val docRef = db.collection("rooms").document(chatRoomName)
        docRef.addSnapshotListener{snapshot,e ->
            if(e != null ){
                Log.d("SNPSHT", "Listen Failed", e)
                return@addSnapshotListener
            }

            val source = if(snapshot != null && snapshot.metadata.hasPendingWrites()) {
                "Local"
            }else {
                "Server"
            }

            if(snapshot != null && snapshot.exists()) {
                chatState.message = snapshot.data.toString()
            }
        }
    }

    fun sortMapOnDate(unsortedMap: Map<String, Map<String, Any>>): Map<String, Map<String, Any>> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        return unsortedMap.entries
            .sortedBy { entry ->
                dateFormat.parse(entry.value["Date"] as String)?.time
            }
            .associate { it.key to it.value }
            .toMap(LinkedHashMap())
    }


}

