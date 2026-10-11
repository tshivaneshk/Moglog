package com.skech.moglog.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class GoogleAuthClient(
    private val context: Context,
    private val auth: FirebaseAuth
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): SignInResult {
        return try {
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val webClientId = "819495450860-1937v953ljsnkn3tt7tto7d7scfc5t1j.apps.googleusercontent.com"

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setNonce(hashedNonce)
                .build()

            val signInOption = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(webClientId)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is androidx.credentials.CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user

                if (user != null && googleIdTokenCredential.profilePictureUri != null) {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setPhotoUri(googleIdTokenCredential.profilePictureUri)
                        .build()
                    user.updateProfile(profileUpdates).await()
                }

                SignInResult(
                    data = user,
                    errorMessage = null
                )
            } else if (credential is GoogleIdTokenCredential) {
                // Fallback just in case
                val idToken = credential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                
                SignInResult(
                    data = authResult.user,
                    errorMessage = null
                )
            } else {
                SignInResult(null, "Received an invalid credential type: ${credential.type}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SignInResult(null, e.message)
        }
    }

    fun getSignedInUser(): FirebaseUser? = auth.currentUser

    fun signOut() {
        auth.signOut()
    }
}

data class SignInResult(
    val data: FirebaseUser?,
    val errorMessage: String?
)
