package com.rdragon.movienotes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    companion object {
        private const val RC_SIGN_IN = 1001
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var viewModel: MovieViewModel

    private lateinit var btnGoogle: ImageView
    private lateinit var searchInput: TextInputEditText
    private lateinit var addBtn: ImageView
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var searchLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_content)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

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
                googleSignInClient.signOut().addOnCompleteListener {
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

        viewModel.movies.observe(this, Observer { list ->
            adapter.submitList(list ?: emptyList())
        })

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
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            if (resultCode != Activity.RESULT_OK) {
                btnGoogle.isEnabled = true
                return
            }
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)!!
                val cred = GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(cred)
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
            }
        }
    }
}
