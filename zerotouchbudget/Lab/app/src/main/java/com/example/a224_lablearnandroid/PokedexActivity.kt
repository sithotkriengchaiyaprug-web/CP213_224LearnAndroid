package com.example.a224_lablearnandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.a224_lablearnandroid.utils.PokemonEntry

// à¸ªà¸µà¹‚à¸—à¸™ Pokedex
val PokedexRed = Color(0xFFC60808)
val DarkRed = Color(0xFF8E0606)
val LightGrayBackground = Color(0xFFE0E0E0)

class ListActivity3 : ComponentActivity() {
    // à¹ƒà¸Šà¹‰ viewModels() delegate à¹€à¸žà¸·à¹ˆà¸­à¹ƒà¸«à¹‰ Android à¸ˆà¸±à¸”à¸à¸²à¸£ Lifecycle à¸‚à¸­à¸‡ ViewModel à¹ƒà¸«à¹‰
    private val viewModel: PokemonViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // à¹€à¸£à¸µà¸¢à¸à¸”à¸¶à¸‡à¸‚à¹‰à¸­à¸¡à¸¹à¸¥à¹€à¸¡à¸·à¹ˆà¸­ Activity à¸–à¸¹à¸à¸ªà¸£à¹‰à¸²à¸‡
        viewModel.fetchPokemon()

        setContent {
            ListScreen(viewModel)
        }
    }
}

@Composable
fun ListScreen(viewModel: PokemonViewModel) {
    // à¹€à¸Šà¸·à¹ˆà¸­à¸¡à¸•à¹ˆà¸­ State à¸ˆà¸²à¸ ViewModel
    val pokemonList by viewModel.pokemonList.collectAsState()
    var searchText by remember { mutableStateOf("") }

    // à¸à¸£à¸­à¸‡à¸‚à¹‰à¸­à¸¡à¸¹à¸¥à¸ˆà¸²à¸ List à¸—à¸µà¹ˆà¹„à¸”à¹‰à¸¡à¸²à¸ˆà¸²à¸ API
    val filteredPokemon = pokemonList.filter {
        it.pokemon_species.name.contains(searchText, ignoreCase = true) ||
                it.entry_number.toString().contains(searchText)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PokedexRed)
            .statusBarsPadding()
    ) {
        HeaderWithSearch(
            searchText = searchText,
            onSearchChange = { searchText = it }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(LightGrayBackground)
                .padding(12.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredPokemon) { entry ->
                    PokemonRow(entry)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = Color.LightGray
                    )
                }

                if (filteredPokemon.isEmpty() && pokemonList.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No Pokemon found", color = Color.Gray)
                        }
                    }
                }

                // à¸à¸£à¸“à¸µà¸—à¸µà¹ˆ List à¸¢à¸±à¸‡à¸§à¹ˆà¸²à¸‡à¹€à¸›à¸¥à¹ˆà¸² (à¸à¸³à¸¥à¸±à¸‡à¹‚à¸«à¸¥à¸”)
                if (pokemonList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PokedexRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PokemonRow(entry: PokemonEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${entry.entry_number}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.width(60.dp)
        )

        Text(
            text = entry.pokemon_species.name.replaceFirstChar { it.uppercase() },
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // à¹ƒà¸Šà¹‰ entry_number à¹ƒà¸™à¸à¸²à¸£à¸”à¸¶à¸‡à¸£à¸¹à¸›à¸ à¸²à¸žà¸ˆà¸²à¸ Server
        AsyncImage(
            model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${entry.entry_number}.png",
            contentDescription = entry.pokemon_species.name,
            modifier = Modifier.size(60.dp),
            contentScale = ContentScale.Fit
        )
    }
}

// --- à¸ªà¹ˆà¸§à¸™ HeaderWithSearch à¹à¸¥à¸° SmallLight à¸„à¸‡à¹€à¸”à¸´à¸¡à¸•à¸²à¸¡à¸—à¸µà¹ˆà¸„à¸¸à¸“à¹€à¸‚à¸µà¸¢à¸™à¹„à¸§à¹‰ ---
@Composable
fun HeaderWithSearch(searchText: String, onSearchChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(65.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF3F51B5)),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0x80FFFFFF)))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallLight(Color.Red)
                SmallLight(Color.Yellow)
                SmallLight(Color.Green)
            }

            TextField(
                value = searchText,
                onValueChange = onSearchChange,
                placeholder = { Text("Search name or ID", color = Color(0xFFC58A8A), fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkRed,
                    unfocusedContainerColor = DarkRed,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        }
    }
}

@Composable
fun SmallLight(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
            .background(Color(0x33FFFFFF))
    )
}