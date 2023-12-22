package com.example.contactlist_dwahlandt23

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contactlist_dwahlandt23.MainActivity.Companion.contactList
import com.example.contactlist_dwahlandt23.ui.theme.ContactList_dwahlandt23Theme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.auth.User
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    companion object{
        lateinit var contactList: ArrayList<Contact>
    }

    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ContactList_dwahlandt23Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    auth = Firebase.auth
                    UserCredentials.username = auth.currentUser?.displayName
                    UserCredentials.userId = auth.currentUser?.uid
                    if(auth.currentUser == null) {
                            val intent = Intent(context, SignInActivity::class.java)
                            context.startActivity(intent)
                        //startActivity(Intent(context, SignInActivity::class.java))
                    }else {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Header(auth, context)
                            ContactList(contentResolver = contentResolver)
                        }
                    }
                }
            }
        }

        contactList = arrayListOf()
    }
}


fun getContacts(contentResolver: ContentResolver): List<Contact> {
    val contact = mutableListOf<Contact>()

    val projection = arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.DISPLAY_NAME,
    )

    val cursor = contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        projection,
        null,
        null
    )

    cursor?.use{
        val idColumn = cursor.getColumnIndex(ContactsContract.Contacts._ID)
        val nameColumn = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)

            contact.add(Contact(id, name))
            contactList.add(Contact(id, name))
        }
    }
    return contact
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ContactList(contentResolver: ContentResolver) {

    val contactsPermission = rememberPermissionState(

        Manifest.permission.READ_CONTACTS
    )

    if(contactsPermission.status.isGranted) {
        val contacts = remember {
            getContacts(contentResolver)
        }

        LazyColumn() {
            items(contacts){contact->
                ContactItem(contact)
            }
        }

    }else {
        Column {
            val textShow = if(contactsPermission.status.shouldShowRationale) {
                "The app needs to read the contact list to be working as intended"
            }else {
                "Contact Permission is required for this feature to work as intended"
            }
            Text(textShow)
            Button(onClick = { contactsPermission.launchPermissionRequest()}) {
                Text(text = "Request permission")
            }
        }

    }
}

@Composable
fun ContactItem(contact: Contact) {
    val mContext = LocalContext.current

    Column(modifier = Modifier.clickable {
        val intent = Intent(mContext, ChatActivity::class.java)
        intent.putExtra("contactId", contact.id)
        mContext.startActivity(intent)

    }) {
        Row(modifier = Modifier.padding(16.dp))  {
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
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        Divider(modifier = Modifier
            .padding(start = 16.dp)
            .padding(end = 16.dp))
    }
}


@Composable
fun Header(auth: FirebaseAuth, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primary),
        horizontalArrangement = Arrangement.Start
            ){
        Text(
            text = "My Contacts",
            style = TextStyle(color = Color.White, fontSize = 24.sp),
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.primary)
                .padding(start = 8.dp)
                .padding(top = 16.dp)
                .padding(bottom = 8.dp),
        )
        Spacer(Modifier.weight(1f))

        Button(onClick = {
            auth.signOut()
            val intent = Intent(context, SignInActivity::class.java)
            context.startActivity(intent)
        },
            modifier = Modifier
            .background(color = MaterialTheme.colorScheme.primary)
            .padding(top = 8.dp),) {
            Text(
                text = "Log out",
                style = TextStyle(color = Color.White, fontSize = 16.sp),
            )
        }
    }
}

