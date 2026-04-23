package com.example.zerotouchbudget.domain.model

enum class AutoScanSource {
    SCREENSHOTS,
    CAMERA,
    CUSTOM_FOLDER;

    fun displayName(): String = when (this) {
        SCREENSHOTS -> "Screenshots"
        CAMERA -> "Camera"
        CUSTOM_FOLDER -> "Custom Folder"
    }

    companion object {
        fun fromStorage(value: String?): AutoScanSource {
            return runCatching {
                value?.let { valueOf(it) }
            }.getOrNull() ?: SCREENSHOTS
        }
    }
}

