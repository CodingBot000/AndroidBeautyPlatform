package com.beauty.platform.utils

import android.content.Context
import java.io.IOException

object JsFileLoader {
    
    private val cache = mutableMapOf<String, String>()
    
    /**
     * assets/js/ 폴더에서 JS 파일 내용을 읽어옴
     * @param context Context
     * @param fileName 파일명 (예: "page_load_detector.js")
     * @return JS 코드 문자열, 실패 시 null
     */
    fun load(context: Context, fileName: String): String? {
        // 캐시 확인
        cache[fileName]?.let { return it }
        
        return try {
            context.assets
                .open("js/$fileName")
                .bufferedReader()
                .use { it.readText() }
                .also { cache[fileName] = it }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 페이지 로드 감지용 JS 로드
     */
    fun loadPageLoadDetector(context: Context): String? {
        return load(context, "page_load_detector.js")
    }
}