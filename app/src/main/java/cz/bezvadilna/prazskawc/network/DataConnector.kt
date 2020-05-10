package cz.bezvadilna.prazskawc.network

import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

const val DATACONNECTOR_LOG_TAG = "DATACONNECTOR_ERROR"

/**
 * Download file from given URL and return it as a String
 * Should be run on IO Dispatcher
 */
suspend fun downloadFileFromUrl(url: String): String? {
    try {
        // Open a stream from the URL
        val stream = URL(url).openStream()
        val result = StringBuilder()
        val reader = BufferedReader(InputStreamReader(stream))

        do {
            reader.readLine()?.let {
                result.append(it)
            } ?: break
        } while (true)

        // Close the stream
        reader.close()
        stream.close()

        return result.toString()
    } catch (e: IOException) {
        Log.e(DATACONNECTOR_LOG_TAG, "Failed or interrupted I/O operations. $e")
    } catch (e: Exception) {
        Log.e(DATACONNECTOR_LOG_TAG, "Failed to get file from URL. $e")
    }

    return null
}