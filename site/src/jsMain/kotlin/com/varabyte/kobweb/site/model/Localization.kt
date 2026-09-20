package com.varabyte.kobweb.site.model

import androidx.compose.runtime.compositionLocalOf

enum class SiteLanguage {
    English,
    Norwegian,
}

val LocalSiteLanguage = compositionLocalOf { SiteLanguage.English }

val LocalSiteLanguageSetter = compositionLocalOf<(SiteLanguage) -> Unit> {
    {}
}

fun SiteLanguage.text(english: String, norwegian: String): String {
    return when (this) {
        SiteLanguage.English -> english
        SiteLanguage.Norwegian -> norwegian
    }
}
