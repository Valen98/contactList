package com.example.contactlist_dwahlandt23

import com.google.firebase.firestore.FirebaseFirestore

class ChatUiState(
    var db: FirebaseFirestore? = null,
    var id: Int = 0,
    var chatName: String = "",
    var message: String = "",
    var phoneId: String = "",
    var sender: String = "",
    var messagesMap: Map<String, Map<String, Any>>? = null
)
