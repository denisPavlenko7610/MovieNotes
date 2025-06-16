package com.rdragon.movienotes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        findViewById<Button>(R.id.btnGoogle).setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }
        findViewById<Button>(R.id.btnGuest).setOnClickListener {
            // Заходим как гость
            MainActivity.start(this, skipSync = true)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)!!
                val cred = GoogleAuthProvider.getCredential(account.idToken, null)
                FirebaseAuth.getInstance()
                    .signInWithCredential(cred)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            // После успешного входа — в MainActivity с синхронизацией
                            MainActivity.start(this, skipSync = false)
                        }
                    }
            } catch (e: ApiException) {
            }
        }
    }

    companion object {
        private const val RC_SIGN_IN = 1001

        fun start(activity: Activity) {
            activity.startActivity(Intent(activity, AuthActivity::class.java))
        }
    }
}
