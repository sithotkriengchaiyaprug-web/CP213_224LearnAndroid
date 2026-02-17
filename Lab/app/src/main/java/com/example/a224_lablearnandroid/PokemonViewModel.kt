package com.example.a224_lablearnandroid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a224_lablearnandroid.utils.PokemonNetwork
import com.example.a224_lablearnandroid.utils.PokemonEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PokemonViewModel : ViewModel() {
    private val _pokemonList = MutableStateFlow<List<PokemonEntry>>(emptyList())
    val pokemonList: StateFlow<List<PokemonEntry>> = _pokemonList

    init {
        fetchPokemon()
    }

    private fun fetchPokemon() {
        viewModelScope.launch {
            try {
                // ดึงข้อมูลจาก API ผ่าน Object ที่คุณสร้างไว้ใน utils
                val response = PokemonNetwork.api.getKantoPokedex()
                _pokemonList.value = response.pokemon_entries
            } catch (e: Exception) {
                // จัดการ Error เช่น กรณีไม่มี Internet
            }
        }
    }
}