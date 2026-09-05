package com.example.nexus.core.cognitive.context

enum class TrustLabel(val authorityLevel: Int, val description: String) {
    SYSTEM(100, "System invariants, sandbox constraints, and security policies"),
    VERIFIED_TOOL(90, "Authoritative tool receipts and physical device sensors"),
    USER(80, "Explicit user instructions, preferences, and corrections"),
    LOCAL_MEMORY(60, "Persistent historical SQLite memory with decay weighting"),
    UNTRUSTED_EXTERNAL(30, "External documents, unverified web or network data"),
    MODEL_GENERATED(10, "Model thoughts and claims — NEVER AUTHORITATIVE")
}

data class ScoredContextItem(
    val id: String,
    val content: String,
    val trustLabel: TrustLabel,
    val relevanceScore: Float,
    val confidence: Float,
    val freshnessScore: Float,
    val compositeRank: Float,
    val estimatedTokens: Int
)
