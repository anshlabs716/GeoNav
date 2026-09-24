package org.geonav.app.data.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.geonav.app.data.model.GeoPoint
import org.geonav.app.data.model.Place
import org.geonav.app.data.model.PlaceCategory
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale

sealed class ExchangeResult<out T> {
    data class Success<T>(val data: T) : ExchangeResult<T>()
    data class Error(val message: String) : ExchangeResult<Nothing>()
}

class GeoNavDataExchangeManager {

    suspend fun exportGpx(places: List<Place>, outputStream: OutputStream): ExchangeResult<Int> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            sb.append("<gpx version=\"1.1\" creator=\"GeoNav\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
            sb.append("  <metadata>\n    <name>GeoNav Saved Places</name>\n  </metadata>\n")

            for (p in places) {
                sb.append(String.format(Locale.US, "  <wpt lat=\"%.6f\" lon=\"%.6f\">\n", p.location.latitude, p.location.longitude))
                sb.append("    <name>${escapeXml(p.name)}</name>\n")
                sb.append("    <desc>${escapeXml(p.address)}</desc>\n")
                sb.append("    <type>${p.category.name}</type>\n")
                sb.append("  </wpt>\n")
            }
            sb.append("</gpx>")

            outputStream.bufferedWriter().use { it.write(sb.toString()) }
            ExchangeResult.Success(places.size)
        } catch (e: Exception) {
            ExchangeResult.Error("Failed to export GPX: ${e.localizedMessage}")
        }
    }

    suspend fun exportKml(places: List<Place>, outputStream: OutputStream): ExchangeResult<Int> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n  <Document>\n")
            sb.append("    <name>GeoNav Saved Places</name>\n")

            for (p in places) {
                sb.append("    <Placemark>\n")
                sb.append("      <name>${escapeXml(p.name)}</name>\n")
                sb.append("      <description>${escapeXml(p.address)}</description>\n")
                sb.append("      <Point>\n")
                sb.append(String.format(Locale.US, "        <coordinates>%.6f,%.6f,0</coordinates>\n", p.location.longitude, p.location.latitude))
                sb.append("      </Point>\n")
                sb.append("    </Placemark>\n")
            }
            sb.append("  </Document>\n</kml>")

            outputStream.bufferedWriter().use { it.write(sb.toString()) }
            ExchangeResult.Success(places.size)
        } catch (e: Exception) {
            ExchangeResult.Error("Failed to export KML: ${e.localizedMessage}")
        }
    }

    suspend fun exportGeoJson(places: List<Place>, outputStream: OutputStream): ExchangeResult<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("type", "FeatureCollection")
            val features = JSONArray()

            for (p in places) {
                val feat = JSONObject()
                feat.put("type", "Feature")
                val geom = JSONObject()
                geom.put("type", "Point")
                val coords = JSONArray()
                coords.put(p.location.longitude)
                coords.put(p.location.latitude)
                geom.put("coordinates", coords)
                feat.put("geometry", geom)

                val props = JSONObject()
                props.put("id", p.id)
                props.put("name", p.name)
                props.put("address", p.address)
                props.put("category", p.category.name)
                feat.put("properties", props)

                features.put(feat)
            }
            root.put("features", features)

            outputStream.bufferedWriter().use { it.write(root.toString(2)) }
            ExchangeResult.Success(places.size)
        } catch (e: Exception) {
            ExchangeResult.Error("Failed to export GeoJSON: ${e.localizedMessage}")
        }
    }

    suspend fun exportCsv(places: List<Place>, outputStream: OutputStream): ExchangeResult<Int> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            sb.append("Name,Latitude,Longitude,Address,Category\n")
            for (p in places) {
                sb.append("\"${escapeCsv(p.name)}\",")
                sb.append(String.format(Locale.US, "%.6f,%.6f,", p.location.latitude, p.location.longitude))
                sb.append("\"${escapeCsv(p.address)}\",")
                sb.append("\"${p.category.name}\"\n")
            }
            outputStream.bufferedWriter().use { it.write(sb.toString()) }
            ExchangeResult.Success(places.size)
        } catch (e: Exception) {
            ExchangeResult.Error("Failed to export CSV: ${e.localizedMessage}")
        }
    }

    suspend fun importPlaces(inputStream: InputStream, format: String): ExchangeResult<List<Place>> = withContext(Dispatchers.IO) {
        try {
            val content = inputStream.bufferedReader().use { it.readText() }
            when (format.uppercase(Locale.US)) {
                "GPX" -> parseGpx(content)
                "KML" -> parseKml(content)
                "GEOJSON", "JSON" -> parseGeoJson(content)
                "CSV" -> parseCsv(content)
                else -> ExchangeResult.Error("Unsupported file format: $format. Supported: GPX, KML, GeoJSON, CSV")
            }
        } catch (e: Exception) {
            ExchangeResult.Error("Failed to import: ${e.localizedMessage}")
        }
    }

    private fun parseGpx(xml: String): ExchangeResult<List<Place>> {
        val places = mutableListOf<Place>()
        val wptRegex = Regex("""<wpt\s+lat="([-+]?\d*\.?\d+)"\s+lon="([-+]?\d*\.?\d+)"[^>]*>(.*?)</wpt>""", RegexOption.DOT_MATCHES_ALL)
        val nameRegex = Regex("""<name>(.*?)</name>""")
        val descRegex = Regex("""<desc>(.*?)</desc>""")

        for (match in wptRegex.findAll(xml)) {
            val lat = match.groupValues[1].toDoubleOrNull() ?: continue
            val lon = match.groupValues[2].toDoubleOrNull() ?: continue
            val body = match.groupValues[3]
            val name = nameRegex.find(body)?.groupValues?.get(1)?.trim() ?: "Imported Waypoint"
            val desc = descRegex.find(body)?.groupValues?.get(1)?.trim() ?: ""

            places.add(
                Place(
                    id = "import_gpx_${System.currentTimeMillis()}_${places.size}",
                    name = name,
                    category = PlaceCategory.GENERAL,
                    address = desc,
                    location = GeoPoint(lat, lon)
                )
            )
        }
        return ExchangeResult.Success(places)
    }

    private fun parseKml(kml: String): ExchangeResult<List<Place>> {
        val places = mutableListOf<Place>()
        val placemarkRegex = Regex("""<Placemark[^>]*>(.*?)</Placemark>""", RegexOption.DOT_MATCHES_ALL)
        val nameRegex = Regex("""<name>(.*?)</name>""")
        val descRegex = Regex("""<description>(.*?)</description>""")
        val coordsRegex = Regex("""<coordinates>([-+]?\d*\.?\d+),([-+]?\d*\.?\d+)""")

        for (match in placemarkRegex.findAll(kml)) {
            val body = match.groupValues[1]
            val name = nameRegex.find(body)?.groupValues?.get(1)?.trim() ?: "Imported Placemark"
            val desc = descRegex.find(body)?.groupValues?.get(1)?.trim() ?: ""
            val coordsMatch = coordsRegex.find(body) ?: continue
            val lon = coordsMatch.groupValues[1].toDoubleOrNull() ?: continue
            val lat = coordsMatch.groupValues[2].toDoubleOrNull() ?: continue

            places.add(
                Place(
                    id = "import_kml_${System.currentTimeMillis()}_${places.size}",
                    name = name,
                    category = PlaceCategory.GENERAL,
                    address = desc,
                    location = GeoPoint(lat, lon)
                )
            )
        }
        return ExchangeResult.Success(places)
    }

    private fun parseGeoJson(jsonStr: String): ExchangeResult<List<Place>> {
        val places = mutableListOf<Place>()
        val root = JSONObject(jsonStr)
        val features = root.optJSONArray("features") ?: return ExchangeResult.Success(emptyList())

        for (i in 0 until features.length()) {
            val feat = features.getJSONObject(i)
            val geom = feat.optJSONObject("geometry") ?: continue
            if (geom.optString("type") == "Point") {
                val coords = geom.optJSONArray("coordinates") ?: continue
                if (coords.length() >= 2) {
                    val lon = coords.getDouble(0)
                    val lat = coords.getDouble(1)
                    val props = feat.optJSONObject("properties")
                    val name = props?.optString("name", "Imported Point") ?: "Imported Point"
                    val address = props?.optString("address", "") ?: ""

                    places.add(
                        Place(
                            id = "import_json_${System.currentTimeMillis()}_$i",
                            name = name,
                            category = PlaceCategory.GENERAL,
                            address = address,
                            location = GeoPoint(lat, lon)
                        )
                    )
                }
            }
        }
        return ExchangeResult.Success(places)
    }

    private fun parseCsv(csv: String): ExchangeResult<List<Place>> {
        val places = mutableListOf<Place>()
        val lines = csv.lines()
        for ((idx, line) in lines.withIndex()) {
            if (idx == 0 || line.isBlank()) continue
            val parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
            if (parts.size >= 3) {
                val name = parts[0].trim('"', ' ')
                val lat = parts[1].trim().toDoubleOrNull() ?: continue
                val lon = parts[2].trim().toDoubleOrNull() ?: continue
                val addr = if (parts.size > 3) parts[3].trim('"', ' ') else ""

                places.add(
                    Place(
                        id = "import_csv_${System.currentTimeMillis()}_$idx",
                        name = name,
                        category = PlaceCategory.GENERAL,
                        address = addr,
                        location = GeoPoint(lat, lon)
                    )
                )
            }
        }
        return ExchangeResult.Success(places)
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun escapeCsv(str: String): String {
        return str.replace("\"", "\"\"")
    }
}
