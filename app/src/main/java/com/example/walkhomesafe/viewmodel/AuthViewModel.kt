package com.example.walkhomesafe.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.example.walkhomesafe.services.UserService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * State representing the current authentication status.
 *
 * @property firebaseUser The current Firebase user, or null if not authenticated
 * @property isEmailVerified Whether the user's email has been verified
 * @property username Display name of the user, or null if not set
 */
data class AuthState(
    val firebaseUser: FirebaseUser?,
    val isEmailVerified: Boolean,
    val username: String? = null
)

/**
 * ViewModel handling all authentication operations: registration, login,
 * email verification, password reset, account deletion, and re-authentication.
 *
 * @property authState StateFlow emitting the current authentication state
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow(
        AuthState(auth.currentUser, auth.currentUser?.isEmailVerified == true, auth.currentUser?.displayName)
    )
    val authState: StateFlow<AuthState> = _authState

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _authState.value = AuthState(user, user?.isEmailVerified == true, user?.displayName)
        }
    }

    /**
     * Registers a new user with email, password, and username.
     * Creates a Firebase Auth account, updates the profile, stores user data in Firestore,
     * and sends an email verification message.
     *
     * @param email The email address for registration
     * @param password The password (min 6 characters)
     * @param username The display name for the user
     * @param onResult Callback with (success, errorMessage)
     */
    fun register(email: String, password: String, username: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser ?: run {
                        onResult(false, "Benutzer konnte nicht erstellt werden")
                        return@addOnCompleteListener
                    }
                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()
                    user.updateProfile(profileUpdate)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                val userData = hashMapOf(
                                    "username" to username,
                                    "email" to email
                                )
                                firestore.collection("users")
                                    .document(user.uid)
                                    .set(userData)
                                    .addOnCompleteListener {
                                        user.sendEmailVerification()
                                        onResult(true, null)
                                    }
                            } else {
                                onResult(false, profileTask.exception?.message ?: "Profil-Update fehlgeschlagen")
                            }
                        }
                } else {
                    onResult(false, localizedError(task.exception))
                }
            }
    }

    /**
     * Signs in an existing user with email and password.
     *
     * @param email The user's email
     * @param password The user's password
     * @param onResult Callback with (success, errorMessage)
     */
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, localizedError(task.exception))
                }
            }
    }

    /**
     * Maps Firebase authentication exceptions to localized German error messages.
     *
     * @param exception The throwable from a Firebase Auth operation, or null
     * @return A human-readable error message in German
     */
    private fun localizedError(exception: Throwable?): String {
        return when (exception) {
            is FirebaseAuthWeakPasswordException -> "Das Passwort muss mindestens 6 Zeichen lang sein"
            is FirebaseAuthInvalidUserException -> "Kein Konto mit dieser E-Mail-Adresse gefunden"
            is FirebaseAuthInvalidCredentialsException -> "Falsche E-Mail oder falsches Passwort"
            is FirebaseAuthUserCollisionException -> "Diese E-Mail-Adresse wird bereits verwendet"
            is FirebaseTooManyRequestsException -> "Zu viele fehlgeschlagene Versuche. Bitte später erneut versuchen"
            else -> exception?.message ?: "Ein Fehler ist aufgetreten"
        }
    }

    /**
     * Resends the email verification message to the current user.
     *
     * @param onResult Callback with (success, errorMessage)
     */
    fun resendVerificationEmail(onResult: (Boolean, String?) -> Unit) {
        auth.currentUser?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception?.message)
            } ?: onResult(false, "Kein Benutzer angemeldet")
    }

    /**
     * Reloads the current user and checks whether the email has been verified.
     *
     * @param onResult Callback with true if email is verified, false otherwise
     */
    fun checkEmailVerified(onResult: (Boolean) -> Unit) {
        auth.currentUser?.reload()?.addOnCompleteListener {
            val user = auth.currentUser
            _authState.value = AuthState(user, user?.isEmailVerified == true, user?.displayName)
            onResult(user?.isEmailVerified == true)
        }
    }

    /**
     * Sends a password reset email to the given address.
     *
     * @param email The email address for the password reset
     * @param onResult Callback with (success, errorMessage)
     */
    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception?.message)
            }
    }

    /**
     * Signs out the current user.
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Deletes the current user's account. First calls the backend UserService,
     * then deletes the Firebase Auth account.
     * If re-authentication is required, the callback receives "RECENT_LOGIN_REQUIRED".
     *
     * @param onResult Callback with (success, errorMessage)
     */
    fun deleteAccount(onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: run {
            onResult(false, "Kein Benutzer angemeldet")
            return
        }
        UserService.deleteUser { _, _ ->
            user.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        val message = task.exception?.message ?: "Unbekannter Fehler"
                        if (task.exception is FirebaseAuthRecentLoginRequiredException) {
                            onResult(false, "RECENT_LOGIN_REQUIRED")
                        } else {
                            onResult(false, message)
                        }
                    }
                }
        }
    }

    /**
     * Re-authenticates the user with email and password, then deletes the account.
     *
     * @param email The user's email for re-authentication
     * @param password The user's password for re-authentication
     * @param onResult Callback with (success, errorMessage)
     */
    fun reauthenticateAndDelete(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: run {
            onResult(false, "Kein Benutzer angemeldet")
            return
        }
        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    UserService.deleteUser { _, _ ->
                        user.delete()
                            .addOnCompleteListener { deleteTask ->
                                if (deleteTask.isSuccessful) {
                                    onResult(true, null)
                                } else {
                                    onResult(false, deleteTask.exception?.message ?: "Fehler beim Löschen")
                                }
                            }
                    }
                } else {
                    onResult(false, reauthTask.exception?.message ?: "Re-Authentifizierung fehlgeschlagen")
                }
            }
    }
}
