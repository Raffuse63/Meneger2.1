package com.example.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthHelper {

    private fun getAuthInstance(context: Context? = null): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            if (context != null) {
                try {
                    FirebaseApp.initializeApp(context)
                    FirebaseAuth.getInstance()
                } catch (e2: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    val currentUser: FirebaseUser?
        get() = try {
            getAuthInstance()?.currentUser
        } catch (e: Exception) {
            null
        }

    companion object {
        const val WEB_CLIENT_ID = "989102272624-n5gu65hmac1t1r9fh4f9bd4cubp7uuvp.apps.googleusercontent.com"
    }

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            val auth = getAuthInstance(context)
                ?: return Result.failure(Exception("Firebase is not initialized"))

            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()

            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Firebase user is null after sign in"))
            }
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            val userMsg = when {
                e.type.contains("NoCredentialException", ignoreCase = true) || e.message?.contains("No credentials", ignoreCase = true) == true ->
                    "ডিভাইসে কোনো গুগল অ্যাকাউন্ট সেটআপ করা নেই অথবা Firebase Console-এ SHA-1 ফিঙ্গারপ্রিন্ট যুক্ত করা হয়নি।"
                else -> e.localizedMessage ?: "গুগল সাইন-ইন ব্যাহত হয়েছে"
            }
            Result.failure(Exception(userMsg, e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            getAuthInstance()?.signOut()
        } catch (e: Exception) {
            // Safe ignore
        }
    }
}
