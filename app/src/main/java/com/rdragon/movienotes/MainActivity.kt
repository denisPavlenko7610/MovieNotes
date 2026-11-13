package com.rdragon.movienotes

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var viewModel: MovieViewModel

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    private lateinit var btnLogout: MaterialButton
    private lateinit var userEmailText: TextView
    private lateinit var authCard: MaterialCardView
    private lateinit var btnGoogle: MaterialButton

    private lateinit var mainContentGroup: Group
    private lateinit var searchInput: TextInputEditText
    private lateinit var addBtn: MaterialButton
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var emptyStateView: TextView

    private lateinit var adapter: MovieAdapter

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        btnGoogle.isEnabled = true
        if (result.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }

        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken

            if (idToken.isNullOrEmpty()) {
                Toast.makeText(this, "Failed to get ID Token", Toast.LENGTH_LONG).show()
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
                }
        } catch (e: ApiException) {
            if (e.statusCode != CommonStatusCodes.CANCELED) {
                Toast.makeText(
                    this,
                    "Google sign-in failed: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_content)

        auth = FirebaseAuth.getInstance()
        viewModel = ViewModelProvider(this)[MovieViewModel::class.java]

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

        btnLogout        = findViewById(R.id.btnLogout)
        userEmailText    = findViewById(R.id.userEmailText)
        authCard         = findViewById(R.id.authCard)
        btnGoogle        = findViewById(R.id.btnGoogle)
        mainContentGroup = findViewById(R.id.mainContentGroup)
        searchInput      = findViewById(R.id.searchInput)
        addBtn           = findViewById(R.id.addBtn)
        recyclerView     = findViewById(R.id.recyclerView)
        emptyStateView   = findViewById(R.id.emptyStateView)

        setupRecyclerView()

        btnGoogle.setOnClickListener {
            btnGoogle.isEnabled = false
            launchSignIn()
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            oneTapClient.signOut()
            viewModel.clearLocalData()
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

    private fun setupRecyclerView() {
        adapter = MovieAdapter(
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
            val movies = list ?: emptyList()
            adapter.submitList(movies)
            emptyStateView.visibility = if (movies.isEmpty() && auth.currentUser != null) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun updateUI() {
        val user = auth.currentUser
        if (user == null) {
            authCard.visibility = View.VISIBLE
            mainContentGroup.visibility = View.GONE
            btnLogout.visibility = View.GONE
            userEmailText.visibility = View.GONE
            emptyStateView.visibility = View.GONE
            btnGoogle.isEnabled = true
        } else {
            authCard.visibility = View.GONE
            mainContentGroup.visibility = View.VISIBLE
            btnLogout.visibility = View.VISIBLE
            userEmailText.visibility = View.VISIBLE
            userEmailText.text = user.email ?: user.displayName ?: ""

            viewModel.syncFromRemote()
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
            }
    }
}
