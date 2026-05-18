package com.finansialku.app.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.finansialku.app.data.entity.UserEntity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
) {

    /**
     * Returns the currently signed-in Firebase user, or null if not signed in.
     */
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    /**
     * Check if user is already authenticated (persistent session).
     */
    fun isUserSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Get the current user's UID.
     */
    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    /**
     * Initiates Google Sign-In using Credential Manager and authenticates with Firebase.
     *
     * @param context Activity context required for Credential Manager
     * @param webClientId The OAuth 2.0 Web Client ID from Firebase Console
     * @return Result with UserEntity on success, or exception on failure
     */
    suspend fun signInWithGoogle(
        context: Context,
        webClientId: String
    ): Result<UserEntity> {
        return try {
            val credentialManager = CredentialManager.create(context)

            // Configure Google ID option
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            // Build credential request
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Get credential from Credential Manager
            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            // Handle the credential result
            handleSignInResult(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Process the credential response and authenticate with Firebase.
     */
    private suspend fun handleSignInResult(
        result: GetCredentialResponse
    ): Result<UserEntity> {
        val credential = result.credential

        return when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential
                        .createFrom(credential.data)

                    // Authenticate with Firebase using the Google ID token
                    val firebaseCredential = GoogleAuthProvider.getCredential(
                        googleIdTokenCredential.idToken,
                        null
                    )

                    val authResult = firebaseAuth
                        .signInWithCredential(firebaseCredential)
                        .await()

                    val firebaseUser = authResult.user
                        ?: return Result.failure(Exception("Firebase authentication failed"))

                    // Save user to local Room database
                    val userEntity = UserEntity(
                        id = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString()
                    )
                    userRepository.insertUser(userEntity)

                    Result.success(userEntity)
                } else {
                    Result.failure(Exception("Unexpected credential type: ${credential.type}"))
                }
            }
            else -> {
                Result.failure(Exception("Unexpected credential type"))
            }
        }
    }

    /**
     * Sign out from Firebase Auth.
     */
    suspend fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Delete all local user data and sign out.
     */
    suspend fun signOutAndClearData(userId: String) {
        userRepository.deleteUser(userId)
        firebaseAuth.signOut()
    }
}
