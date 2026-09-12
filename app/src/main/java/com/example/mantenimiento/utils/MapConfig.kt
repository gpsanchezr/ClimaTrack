package com.example.mantenimiento.utils

import android.content.Context
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import java.io.File

/**
 * Configuración centralizada de OSMdroid para toda la aplicación ClimaTrack
 * (ClientMapActivity, GeolocationActivity y módulos de gestión).
 *
 * Configurada con CARTO Voyager y la API Key oficial para eliminar la marca
 * de agua "API KEY REQUIRED" y prevenir bloqueos 403. Funciona para los
 * tres roles de la plataforma: Cliente, Técnico y Administrador.
 */
object MapConfig {

    /**
     * Interruptor de proveedor de mapa:
     * - false: CARTO Voyager con API Key oficial (sin marca de agua).
     * - true:  Mapnik estándar de OpenStreetMap.
     */
    private const val USAR_MAPNIK = false

    /**
     * API Key oficial de CARTO Voyager.
     */
    private const val CARTO_API_KEY = "cb1_3hqr_1_f219bd58e6278b6a85385a5a"

    /**
     * Inicialización obligatoria para OSMdroid. Debe llamarse en el onCreate()
     * de cada Activity con mapa, ANTES de setContentView().
     *
     * ELIMINA LA CACHÉ ANTIGUA (incluyendo v4 y v5) y crea la versión v6 para
     * forzar el refresco completo exigido por CARTO y borrar definitivamente las
     * teselas cacheadas que contenían la marca de agua.
     */
    fun inicializar(context: Context) {
        val appContext = context.applicationContext

        // FORZAR REFRESCO: Borrado incondicional de las cachés antiguas acumuladas
        val carpetasABorrar = arrayOf(
            File(appContext.filesDir, "osmdroid"),
            File(appContext.filesDir, "osmdroid_cache_v4"),
            File(appContext.filesDir, "osmdroid_cache_v5"),
            File(appContext.cacheDir, "osmdroid"),
            File(appContext.cacheDir, "osmdroid_cache_v4"),
            File(appContext.cacheDir, "osmdroid_cache_v5"),
            File(appContext.cacheDir, "tiles")
        )
        for (folder in carpetasABorrar) {
            if (folder.exists()) {
                folder.deleteRecursively()
            }
        }

        // Crear e indicar a OSMdroid la nueva carpeta de caché v6
        val config = Configuration.getInstance()
        config.userAgentValue = appContext.packageName
        config.osmdroidTileCache = File(appContext.cacheDir, "osmdroid_cache_v6")
        config.load(appContext, PreferenceManager.getDefaultSharedPreferences(appContext))
    }

    /**
     * Retorna la fuente de teselas de CARTO Voyager utilizando los subdominios (a, b, c, d),
     * maxZoom 20, la atribución oficial y el sufijo con la API Key autenticada.
     */
    fun tileSource(): ITileSource {
        if (USAR_MAPNIK) {
            return TileSourceFactory.MAPNIK
        }

        val sufijo = if (CARTO_API_KEY.isBlank()) {
            ".png"
        } else {
            ".png?key=$CARTO_API_KEY"
        }

        return XYTileSource(
            "CartoVoyager",
            0,
            20,
            256,
            sufijo,
            arrayOf(
                "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
            ),
            "© OpenStreetMap contributors, © CARTO"
        )
    }
}