package com.example.a224_lablearnandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

class PokedexActivity : ComponentActivity() {
    private val viewModel: PokemonViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val pokemonList by viewModel.pokemonList.collectAsState()

            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                items(pokemonList) { item ->
                    Row(modifier = Modifier.padding(8.dp)) {
                        // ดึงรูปภาพตาม entry_number
                        val imageUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${item.entry_number}.png"

                        AsyncImage(
                            model = imageUrl,
                            contentDescription = item.pokemon_species.name,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = "${item.entry_number}. ${item.pokemon_species.name.uppercase()}")
                    }
                }
            }
        }
    }
}