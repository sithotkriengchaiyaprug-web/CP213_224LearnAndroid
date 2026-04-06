package com.example.a224_lablearnandroid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a224_lablearnandroid.utils.PokemonEntry
import com.example.a224_lablearnandroid.utils.PokemonNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PokemonViewModel : ViewModel() {

    // State à¸ªà¸³à¸«à¸£à¸±à¸šà¸šà¸­à¸à¸ªà¸–à¸²à¸™à¸°à¸‚à¸­à¸‡à¸«à¸™à¹‰à¸²à¸ˆà¸­ (Loading, Success, Error)
    // à¹ƒà¸™à¸—à¸µà¹ˆà¸™à¸µà¹‰à¹€à¸­à¸²à¹à¸šà¸šà¸‡à¹ˆà¸²à¸¢à¸ªà¸¸à¸”à¸„à¸·à¸­à¹€à¸à¹‡à¸š List à¸‚à¸­à¸‡à¹‚à¸›à¹€à¸à¸¡à¸­à¸™
    private val _pokemonList = MutableStateFlow<List<PokemonEntry>>(emptyList())
    val pokemonList = _pokemonList.asStateFlow()

    // à¸Ÿà¸±à¸‡à¸à¹Œà¸Šà¸±à¸™à¸¢à¸´à¸‡ API
    fun fetchPokemon() {
        viewModelScope.launch {
            try {
                // à¹€à¸£à¸µà¸¢à¸à¹ƒà¸Šà¹‰ API à¸ˆà¸²à¸à¹„à¸Ÿà¸¥à¹Œ PokemonApi.kt à¸—à¸µà¹ˆà¹€à¸£à¸²à¸ªà¸£à¹‰à¸²à¸‡
                val response = PokemonNetwork.api.getKantoPokedex()

                // à¸­à¸±à¸›à¹€à¸”à¸•à¸‚à¹‰à¸­à¸¡à¸¹à¸¥à¹ƒà¸ªà¹ˆ State
                _pokemonList.value = response.pokemon_entries

            } catch (e: Exception) {
                // à¸ˆà¸±à¸”à¸à¸²à¸£ Error (à¹€à¸Šà¹ˆà¸™ Log à¸«à¸£à¸·à¸­à¹‚à¸Šà¸§à¹Œ Toast)
                e.printStackTrace()
            }
        }
    }
}