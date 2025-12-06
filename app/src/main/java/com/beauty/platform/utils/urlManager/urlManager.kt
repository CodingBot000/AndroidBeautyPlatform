package com.beauty.platform.utils.urlManager

import java.net.URI


// 홈으로 간주할 URL path 패턴
 val HOME_URL_PATTERNS = listOf(
    "/",
    "/ko", "/ko/",
    "/en", "/en/",
    "/ja", "/ja/",
    "/zh-CN", "/zh-CN/",
    "/zh-TW", "/zh-TW/"
)


fun isHomeUrl(url: String): Boolean {
    return try {
        val path = URI(url).path ?: "/"
        HOME_URL_PATTERNS.any { pattern ->
            path == pattern || path.isEmpty()
        }
    } catch (e: Exception) {
        false
    }
}