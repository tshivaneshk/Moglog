package com.skech.moglog.domain.model

data class DramaExtraDetails(
    val trailerKey: String?,
    val cast: List<CastMember>
)

data class CastMember(
    val name: String,
    val character: String,
    val profilePath: String?
)
