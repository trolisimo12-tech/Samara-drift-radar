package com.samaradrift.radar

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

object TosApi {
    private const val API_URL = "https://tosamara.ru/api/v2/json"
    private const val SALT = "k290ru5489g"
    private const val CLIENT = "korotkov_samara_transport"

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun call(message: String): String {
        val auth = sha1(message + SALT)
        val body = "message=" + URLEncoder.encode(message, "UTF-8") +
                "&os=Android&clientId=$CLIENT&authKey=$auth"
        val conn = URL(API_URL).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val text = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()
        return text
    }

    fun getTransportsOnRoute(krIds: List<Int>, count: Int = 100): String {
        val kr = krIds.joinToString(",", "[", "]")
        return call("""{"method":"getTransportsOnRoute","KR_ID":$kr,"COUNT":$count}""")
    }

    fun getSurroundingTransports(lat: Double, lon: Double, radius: Int, count: Int): String =
        call("""{"method":"getSurroundingTransports","LATITUDE":$lat,"LONGITUDE":$lon,"RADIUS":$radius,"COUNT":$count}""")
}
