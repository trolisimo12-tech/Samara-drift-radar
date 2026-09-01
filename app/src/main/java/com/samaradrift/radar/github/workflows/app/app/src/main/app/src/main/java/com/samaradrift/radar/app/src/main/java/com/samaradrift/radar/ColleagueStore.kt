package com.samaradrift.radar

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Colleague(
    val name: String,
    val stateNumber: String,
    val krIds: List<Int>
)

object ColleagueStore {
    private lateinit var file: File
    private val colleagues = mutableListOf<Colleague>()

    fun init(ctx: Context) {
        file = File(ctx.filesDir, "colleagues.json")
        if (file.exists()) {
            runCatching {
                val arr = JSONArray(file.readText())
                colleagues.clear()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val kr = mutableListOf<Int>()
                    val ja = o.getJSONArray("krIds")
                    for (k in 0 until ja.length()) kr.add(ja.getInt(k))
                    colleagues.add(Colleague(o.getString("name"), o.getString("board"), kr))
                }
            }
        }
    }

    fun all(): List<Colleague> = colleagues.toList()
    fun boardNumbers(): Set<String> = colleagues.map { it.stateNumber }.toSet()
    fun routeIds(): List<Int> = colleagues.flatMap { it.krIds }.distinct()

    fun add(name: String, board: String, krIds: List<Int>) {
        colleagues.add(Colleague(name, board, krIds))
        save()
    }

    fun remove(pos: Int) {
        if (pos in colleagues.indices) { colleagues.removeAt(pos); save() }
    }

    private fun save() {
        val arr = JSONArray()
        colleagues.forEach { c ->
            arr.put(JSONObject().put("name", c.name).put("board", c.stateNumber).put("krIds", JSONArray(c.krIds)))
        }
        file.writeText(arr.toString())
    }
}
