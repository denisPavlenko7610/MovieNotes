package com.example.movienotes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: MovieAdapter
    private lateinit var viewModel: MovieViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val searchInput  = findViewById<TextInputEditText>(R.id.searchInput)
        val addFab       = findViewById<FloatingActionButton>(R.id.addFab)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        // ViewModel с фабрикой AndroidViewModelFactory
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        viewModel = ViewModelProvider(this, factory)
            .get(MovieViewModel::class.java)

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

        // Подписка на изменения
        viewModel.movies.observe(this) { list ->
            adapter.submitList(list)
        }

        // Поиск по тексту
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        // Добавление новым FAB и по Enter
        addFab.setOnClickListener { addMovie(searchInput) }
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addMovie(searchInput)
                true
            } else false
        }

        // Запустить первую загрузку
        viewModel.loadMovies()
    }

    private fun addMovie(input: TextInputEditText) {
        val title = input.text.toString().trim()
        if (title.isNotEmpty()) {
            viewModel.addNewMovie(title)
            input.text?.clear()
        }
    }
}
