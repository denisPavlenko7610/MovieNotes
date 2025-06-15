package com.rdragon.movienotes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: MovieAdapter
    private lateinit var viewModel: MovieViewModel
    private lateinit var addButton: ImageView

    companion object {
        private const val EXTRA_SKIP_SYNC = "skip_sync"

        fun start(activity: Activity, skipSync: Boolean) {
            activity.startActivity(
                Intent(activity, MainActivity::class.java)
                    .putExtra(EXTRA_SKIP_SYNC, skipSync)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            //Log.d("MainActivity", "User not authenticated, launching AuthActivity")
            AuthActivity.start(this)
            finish()
            return
        } else {
            //Log.d("MainActivity", "User is authenticated: uid=${user.uid}")
        }

        setContentView(R.layout.activity_main)

        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        viewModel = ViewModelProvider(this, factory).get(MovieViewModel::class.java)

        val skipSync = intent.getBooleanExtra(EXTRA_SKIP_SYNC, false)
        if (!skipSync) {
            //Log.d("MainActivity", "Calling viewModel.syncFromRemote() manually")
            viewModel.syncFromRemote()
        }

        setupUI()
    }

    private fun setupUI() {
        val searchInput = findViewById<TextInputEditText>(R.id.searchInput)
        addButton = findViewById(R.id.addBtn)
        addButton.setOnClickListener { addMovie(searchInput) }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
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
            //Log.d("MainActivity", "LiveData movies: size=${list.size} → $list")
            adapter.submitList(list)
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addMovie(searchInput)
                true
            } else false
        }
    }

    private fun addMovie(input: TextInputEditText) {
        val title = input.text.toString().trim()
        if (title.isNotEmpty()) {
            viewModel.addNewMovie(title)
            input.text?.clear()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val skipSync = intent.getBooleanExtra(EXTRA_SKIP_SYNC, false)
        if (!skipSync && ::viewModel.isInitialized) {
            //Log.d("MainActivity", "Syncing changes to Firebase...")
            viewModel.syncToRemote()
            //Log.d("MainActivity", "Sync completed successfully.")
        } else {
            //Log.d("MainActivity", "Skipping sync due to skipSync flag or uninitialized ViewModel.")
        }
    }
}
