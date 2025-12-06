package com.beauty.platform

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Google Credential Manager API를 사용한 Google 로그인 관리자
 */
class GoogleAuthManager(
    private val context: Context,
    private val onSuccess: (String) -> Unit, // ID Token 반환
    private val onError: (String) -> Unit
) {
    private val credentialManager = CredentialManager.create(context)
    
    companion object {
        // Web Client ID from Google Cloud Console579022203717-c25pgsl82dpcfl0gfc11cjt5ngp314sh.apps.googleusercontent.com
        private const val WEB_CLIENT_ID = "579022203717-c25pgsl82dpcfl0gfc11cjt5ngp314sh.apps.googleusercontent.com"
//        private const val WEB_CLIENT_ID = "579022203717-184rbjmujqdq8g3iabkc6uai1uau62tv.apps.googleusercontent.com"
    }

    /**
     * Google 로그인 시작
     */
    fun signIn() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = performGoogleSignIn()
                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(result.credential.data)
                
                val idToken = googleIdTokenCredential.idToken
                Log.d("GoogleAuthManager", "Google Sign-In successful, ID Token received")
                onSuccess(idToken)
                
            } catch (e: GetCredentialException) {
                Log.e("GoogleAuthManager", "Google Sign-In failed", e)
                onError("로그인 실패: ${e.message}")
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("GoogleAuthManager", "Invalid Google ID token", e)
                onError("로그인 토큰 오류: ${e.message}")
            } catch (e: Exception) {
                Log.e("GoogleAuthManager", "Unexpected error during Google Sign-In", e)
                onError("예상치 못한 오류: ${e.message}")
            }
        }
    }

    /**
     * Credential Manager API를 사용하여 Google 로그인 수행
     */
    private suspend fun performGoogleSignIn(): GetCredentialResponse {
        return withContext(Dispatchers.IO) {
            val googleIdOption: CredentialOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            credentialManager.getCredential(
                request = request,
                context = context,
            )
        }
    }
}