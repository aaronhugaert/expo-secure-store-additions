package expo.modules.securestore

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor
import javax.crypto.Cipher
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

fun createBiometricPromptInfo (title: String, cancelString: String, allowDeviceCredentials: Boolean): BiometricPrompt.PromptInfo {
  var authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG;
  var promptInfoBuilder = PromptInfo.Builder()

  if(allowDeviceCredentials && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
    authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL;
  } else {
    promptInfoBuilder.setNegativeButtonText(cancelString)
  }

  promptInfoBuilder.setAllowedAuthenticators(authenticators)
    .setTitle(title)
  
  return promptInfoBuilder.build()
}

class AuthenticationPrompt(private val currentActivity: FragmentActivity, context: Context, title: String, allowDeviceCredentials: Boolean) {
  private var executor: Executor = ContextCompat.getMainExecutor(context)
  private var cancelString = context.getString(android.R.string.cancel);
  private var promptInfo = createBiometricPromptInfo(title, cancelString, allowDeviceCredentials);

  suspend fun authenticate(cipher: Cipher): BiometricPrompt.AuthenticationResult? =
    suspendCoroutine { continuation ->
      BiometricPrompt(
        currentActivity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)

            if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
              continuation.resumeWithException(AuthenticationException("User canceled the authentication"))
            } else {
              continuation.resumeWithException(AuthenticationException("Could not authenticate the user"))
            }
          }

          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            continuation.resume(result)
          }
        }
      ).authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }
}
