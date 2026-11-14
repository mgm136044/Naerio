package app.naerio.data

import kotlinx.serialization.json.Json
import app.naerio.domain.PlaceData

sealed interface LoadResult {
    data class Loaded(val data: PlaceData) : LoadResult
    data class Invalid(val errors: List<String>) : LoadResult
}

/** 앱 내 JSON 장소 데이터. 잘못된 데이터는 앱을 종료시키지 않고 오류 목록으로 돌려준다. */
object PlaceJson {
    val json = Json { ignoreUnknownKeys = false; explicitNulls = false }

    fun parse(text: String): LoadResult = try {
        val data = json.decodeFromString(PlaceData.serializer(), text)
        val errors = PlaceDataValidator.validate(data)
        if (errors.isEmpty()) LoadResult.Loaded(data) else LoadResult.Invalid(errors)
    } catch (e: Exception) {
        LoadResult.Invalid(listOf("JSON을 읽을 수 없습니다: ${e.message?.take(300)}"))
    }
}

class JsonPlaceRepository(private val readAsset: (String) -> String) {
    fun load(path: String = "places/demo_site.json"): LoadResult =
        try { PlaceJson.parse(readAsset(path)) } catch (e: Exception) { LoadResult.Invalid(listOf("파일을 열 수 없습니다: $path")) }
}
