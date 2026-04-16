package com.example.a224_lablearnandroid.architecture.mvi

/**
 * Intent (à¹„à¸¡à¹ˆà¹ƒà¸Šà¹ˆ Intent à¸‚à¸­à¸‡ Android)
 * à¸„à¸·à¸­à¸à¸²à¸£à¸à¸£à¸°à¸—à¸³à¸«à¸£à¸·à¸­à¸„à¸§à¸²à¸¡à¸•à¸±à¹‰à¸‡à¹ƒà¸ˆà¸‚à¸­à¸‡à¸œà¸¹à¹‰à¹ƒà¸Šà¹‰à¸—à¸µà¹ˆà¹€à¸£à¸²à¸à¸³à¸«à¸™à¸”à¸‚à¸¶à¹‰à¸™à¸¡à¸²
 * à¹ƒà¸Šà¹‰ Sealed Class à¹€à¸žà¸·à¹ˆà¸­à¸ˆà¸³à¸à¸±à¸”à¸§à¹ˆà¸²à¸¡à¸µ Action à¸­à¸°à¹„à¸£à¹„à¸”à¹‰à¸šà¹‰à¸²à¸‡
 */
sealed class CounterIntent {
    // à¹ƒà¸™à¸•à¸±à¸§à¸­à¸¢à¹ˆà¸²à¸‡à¸™à¸µà¹‰à¸¡à¸µà¹à¸„à¹ˆ Intent à¹€à¸”à¸µà¸¢à¸§à¸„à¸·à¸­à¸à¸²à¸£à¸à¸”à¸›à¸¸à¹ˆà¸¡à¹€à¸žà¸´à¹ˆà¸¡à¸„à¹ˆà¸²
    object IncrementCounter : CounterIntent()
    object DecrementCounter : CounterIntent()
}