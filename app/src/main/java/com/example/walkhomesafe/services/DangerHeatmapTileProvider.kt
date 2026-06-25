package com.example.walkhomesafe.services

import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.util.Log
import com.example.walkhomesafe.api.ReportDto
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sinh
import kotlin.math.sqrt

/**
 * Custom TileProvider that renders a real-time danger heatmap overlay on Google Maps.
 * Each report contributes positive (danger) or negative (safe) influence based on its ratings.
 *
 * @property reports List of safety reports to render on the heatmap
 * @property influenceRadiusMeters Radius of influence for each report in meters (default 350)
 */
class DangerHeatmapTileProvider(
    private val reports: List<ReportDto>,
    private val influenceRadiusMeters: Double = 350.0
) : TileProvider {

    /**
     * Generates a 256x256 heatmap tile for the given tile coordinates and zoom level.
     * Uses a grid-based bilinear interpolation approach for performance.
     *
     * @param x Tile X coordinate
     * @param y Tile Y coordinate
     * @param zoom Zoom level
     * @return The generated PNG tile, or NO_TILE if no relevant reports exist
     */
    override fun getTile(x: Int, y: Int, zoom: Int): Tile {
        return try {
            val bounds = tileBounds(x, y, zoom)
            val center = LatLng((bounds.north + bounds.south) / 2.0, (bounds.west + bounds.east) / 2.0)
            val halfDiagonal = distanceBetweenInMeters(LatLng(bounds.south, bounds.west), LatLng(bounds.north, bounds.east)) / 2.0

            val relevant = reports.mapNotNull { report ->
                val cats = report.ratingCategories
                if (cats.isNullOrEmpty()) return@mapNotNull null

                val totalStars = cats.sumOf { it.rating }
                val maxStars = cats.size * 5
                val threshold = 9
                val minStars = cats.size

                val isPositive = totalStars > threshold
                val weight = (if (isPositive) {
                    (totalStars - threshold).toDouble() / (maxStars - threshold)
                } else {
                    (threshold - totalStars).toDouble() / (threshold - minStars)
                }).coerceIn(0.0, 1.0)

                val severityFactor = when {
                    weight >= 0.75 -> 2.0
                    weight >= 0.50 -> 1.5
                    weight >= 0.25 -> 1.0
                    else -> 0.5
                }
                val sign = if (isPositive) -1.0 else 1.0

                val latLng = LatLng(report.latitude, report.longitude)
                val dist = distanceBetweenInMeters(latLng, center)
                if (dist > influenceRadiusMeters + halfDiagonal) return@mapNotNull null

                HeatmapEntry(report.latitude, report.longitude, severityFactor, sign)
            }

            if (relevant.isEmpty()) {
                return TileProvider.NO_TILE
            }

            val midLatRad = Math.toRadians((bounds.north + bounds.south) / 2.0)
            val metersPerDeg = 111320.0
            val metersPerDegLng = metersPerDeg * cos(midLatRad)

            val stride = TILE_SIZE / GRID_SIZE
            val grid = Array(GRID_SIZE + 1) { DoubleArray(GRID_SIZE + 1) }

            for (gy in 0..GRID_SIZE) {
                val pixelY = gy * stride
                val lat = bounds.north - (bounds.north - bounds.south) * pixelY / TILE_SIZE

                for (gx in 0..GRID_SIZE) {
                    val pixelX = gx * stride
                    val lng = bounds.west + (bounds.east - bounds.west) * pixelX / TILE_SIZE

                    var netScore = 0.0
                    for (entry in relevant) {
                        val dlat = (entry.latitude - lat) * metersPerDeg
                        val dlng = (entry.longitude - lng) * metersPerDegLng
                        val dist = sqrt(dlat * dlat + dlng * dlng)

                        if (dist > influenceRadiusMeters) continue

                        val influence = gaussianWeight(dist)
                        netScore += entry.sign * influence * entry.severityFactor
                    }
                    grid[gy][gx] = netScore
                }
            }

            val bitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888)
            for (py in 0 until TILE_SIZE) {
                val gy = py / stride
                val yFrac = (py % stride).toDouble() / stride
                val gy1 = minOf(gy + 1, GRID_SIZE)

                for (px in 0 until TILE_SIZE) {
                    val gx = px / stride
                    val xFrac = (px % stride).toDouble() / stride
                    val gx1 = minOf(gx + 1, GRID_SIZE)

                    val top = grid[gy][gx] * (1.0 - xFrac) + grid[gy][gx1] * xFrac
                    val bottom = grid[gy1][gx] * (1.0 - xFrac) + grid[gy1][gx1] * xFrac
                    val score = top * (1.0 - yFrac) + bottom * yFrac

                    bitmap.setPixel(px, py, scoreToColor(score))
                }
            }

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            Tile(TILE_SIZE, TILE_SIZE, stream.toByteArray())
        } catch (e: Exception) {
            Log.e("DangerHeatmapTileProvider", "Failed to generate tile ($x, $y, $zoom)", e)
            TileProvider.NO_TILE
        }
    }

    /**
     * Computes a Gaussian weight for a given distance, used to smooth the heatmap influence.
     * Sigma is set to influenceRadiusMeters / 3.
     *
     * @param distance Distance in meters
     * @return Gaussian weight between 0 and 1
     */
    private fun gaussianWeight(distance: Double): Double {
        val sigma = influenceRadiusMeters / 3.0
        return exp(-(distance * distance) / (2.0 * sigma * sigma))
    }

    companion object {
        private const val TILE_SIZE = 256
        private const val GRID_SIZE = 32
        private const val MAX_SCORE = 2.5
        private const val NEUTRAL_EPSILON = 0.3

        /**
         * Bounding box of a map tile in geographic coordinates.
         *
         * @property south Southern latitude bound
         * @property north Northern latitude bound
         * @property west Western longitude bound
         * @property east Eastern longitude bound
         */
        private data class TileBounds(
            val south: Double,
            val north: Double,
            val west: Double,
            val east: Double
        )

        /**
         * Computes the geographic bounds for a given tile coordinate and zoom level.
         *
         * @param x Tile X coordinate
         * @param y Tile Y coordinate
         * @param zoom Zoom level
         * @return TileBounds with south/north/west/east in degrees
         */
        private fun tileBounds(x: Int, y: Int, zoom: Int): TileBounds {
            val n = 1 shl zoom
            val lonMin = x.toDouble() / n * 360.0 - 180.0
            val lonMax = (x + 1).toDouble() / n * 360.0 - 180.0
            val latMinRad = atan(sinh(PI * (1.0 - 2.0 * (y + 1).toDouble() / n)))
            val latMaxRad = atan(sinh(PI * (1.0 - 2.0 * y.toDouble() / n)))
            return TileBounds(
                south = Math.toDegrees(latMinRad),
                north = Math.toDegrees(latMaxRad),
                west = lonMin,
                east = lonMax
            )
        }

        /**
         * Calculates the distance between two LatLng points in meters using the WGS84 ellipsoid.
         *
         * @param a First coordinate
         * @param b Second coordinate
         * @return Distance in meters
         */
        private fun distanceBetweenInMeters(a: LatLng, b: LatLng): Double {
            val results = FloatArray(1)
            Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
            return results[0].toDouble()
        }

        /**
         * Internal representation of a report's influence on the heatmap.
         *
         * @property latitude Latitude of the report
         * @property longitude Longitude of the report
         * @property severityFactor Weight based on rating severity (0.5-2.0)
         * @property sign +1 for danger (positive score), -1 for safe (negative score)
         */
        private data class HeatmapEntry(
            val latitude: Double,
            val longitude: Double,
            val severityFactor: Double,
            val sign: Double
        )

        /**
         * Maps a cumulative heatmap score to an ARGB color.
         * Positive scores (danger) render as red, negative scores (safe) as green.
         * Scores near zero are transparent.
         *
         * @param score The cumulative score at a pixel
         * @return ARGB color integer
         */
        private fun scoreToColor(score: Double): Int {
            val absScore = abs(score)
            if (absScore < NEUTRAL_EPSILON) {
                return Color.TRANSPARENT
            }

            val clamped = score.coerceIn(-MAX_SCORE, MAX_SCORE)
            val normalized = clamped / MAX_SCORE
            val alpha = (abs(normalized) * 100 + 10).toInt().coerceIn(0, 255)

            return if (score < 0) {
                Color.argb(alpha, 0, 255, 0)
            } else {
                Color.argb(alpha, 255, 0, 0)
            }
        }
    }
}
