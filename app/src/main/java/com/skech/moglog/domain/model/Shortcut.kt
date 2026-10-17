package com.skech.moglog.domain.model

data class Shortcut(
    val id: Int = 0,
    val title: String,
    val data: String, // package name or URL
    val type: String, // "APP" or "LINK"
    val iconUrl: String? = null,
    val dramaId: Int = 0 // 0 = global shortcut, >0 = bound to specific show
)
