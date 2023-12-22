package com.example.contactlist_dwahlandt23

data class SignInState(
    var isSignedInSuccessful: Boolean = false,
    var signInError: String? = null
)
