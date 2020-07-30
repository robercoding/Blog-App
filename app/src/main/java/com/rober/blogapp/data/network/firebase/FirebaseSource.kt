package com.rober.blogapp.data.network.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseSource {

    var db = FirebaseFirestore.getInstance()
    var auth = FirebaseAuth.getInstance()

    var userAuth = auth.currentUser
}