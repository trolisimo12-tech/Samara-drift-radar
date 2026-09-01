package com.samaradrift.radar

import org.json.JSONObject

data class Vehicle(
    val krId: Int,
    val stateNumber: String,
    val model: String,
    val lat: Double,
    val lng: Double,
    val unattached: Boolean
)

data class RadarState(
    val mine: List<Pair<Colleague, Vehicle>>,   // найденные коллеги со своими вагонами
    val others: List<Vehicle>
)

object TransportParser {

    fun parse(json: String, boardSet: Set<String>): RadarState {
        val mine = mutableListOf<Pair<Colleague, Vehicle>>()
        val others = mutableListOf<Vehicle>()
        val arr = JSONObject(json).getJSONArray("transports")

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val v = Vehicle(
                krId = o.getInt("KR_ID"),
                stateNumber = o.getString("stateNumber"),
                model = o.optString("modelTitle"),
                lat = o.getDouble("latitude"),
                lng = o.getDouble("longitude"),
                unattached = o.optBoolean("unattached")
            )
            val colleague = ColleagueStore.all().find { v.stateNumber.startsWith(it.stateNumber) }
            if (colleague != null && v.stateNumber in boardSet) {
                mine.add(colleague to v)
            } else {
                others.add(v)
            }
        }
        return RadarState(mine, others)
    }
}
