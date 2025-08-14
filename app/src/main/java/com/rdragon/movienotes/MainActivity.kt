package com.rdragon.movienotes

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var viewModel: MovieViewModel

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    private lateinit var btnGoogle: ImageView
    private lateinit var searchInput: TextInputEditText
    private lateinit var addBtn: ImageView
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var searchLayout: View

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            btnGoogle.isEnabled = true
            updateUI()
            return@registerForActivityResult
        }

        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken

            if (idToken.isNullOrEmpty()) {
                Toast.makeText(this, "Не удалось получить ID Token", Toast.LENGTH_LONG).show()
                btnGoogle.isEnabled = true
                updateUI()
                return@registerForActivityResult
            }

            val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(firebaseCred)
                .addOnCompleteListener { authTask ->
                    if (!authTask.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Authentication failed: ${authTask.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    btnGoogle.isEnabled = true
                }
        } catch (e: ApiException) {
            if (e.statusCode != CommonStatusCodes.CANCELED) {
                Toast.makeText(
                    this,
                    "Google sign-in failed (code ${e.statusCode}): ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
            btnGoogle.isEnabled = true
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_content)

        auth = FirebaseAuth.getInstance()

        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()

        btnGoogle    = findViewById(R.id.btnGoogle)
        searchLayout = findViewById(R.id.searchLayout)
        searchInput  = findViewById(R.id.searchInput)
        addBtn       = findViewById(R.id.addBtn)
        recyclerView = findViewById(R.id.recyclerView)

        btnGoogle.setOnClickListener {
            btnGoogle.isEnabled = false
            val currentUser = auth.currentUser
            if (currentUser != null) {
                auth.signOut()
                oneTapClient.signOut().addOnCompleteListener {
                    launchSignIn()
                }
            } else {
                launchSignIn()
            }
        }

        authStateListener = FirebaseAuth.AuthStateListener {
            updateUI()
        }

        updateUI()
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun updateUI() {
        val user = auth.currentUser
        if (user == null) {
            searchLayout.visibility = View.GONE
            addBtn.visibility = View.GONE
            recyclerView.visibility = View.GONE
        } else {
            searchLayout.visibility = View.VISIBLE
            addBtn.visibility = View.VISIBLE
            recyclerView.visibility = View.VISIBLE

            val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            viewModel = ViewModelProvider(this, factory).get(MovieViewModel::class.java)

            runBlocking { viewModel.syncFromRemote() }
            setupMainUI()
        }

        btnGoogle.visibility = View.VISIBLE
        btnGoogle.isEnabled = true
    }

    private fun setupMainUI() {
        val adapter = MovieAdapter(
            onCheckedChange = { movie, isChecked ->
                movie.watched = isChecked
                viewModel.updateMovie(movie)
            },
            onDeleteClick = { movie ->
                viewModel.deleteMovie(movie)
            }
        )

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        viewModel.movies.observe(this) { list ->
            adapter.submitList(list ?: emptyList())
        }

        addBtn.setOnClickListener {
            val title = searchInput.text.toString().trim()
            if (title.isNotEmpty()) {
                viewModel.addNewMovie(title)
                searchInput.text?.clear()
            }
        }

        searchInput.doAfterTextChanged { editable ->
            viewModel.setSearchQuery(editable?.toString() ?: "")
        }
    }

    private fun launchSignIn() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                val request = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                signInLauncher.launch(request)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Google Sign-In failed to launch: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
                btnGoogle.isEnabled = true
                updateUI()
            }
    }
}
