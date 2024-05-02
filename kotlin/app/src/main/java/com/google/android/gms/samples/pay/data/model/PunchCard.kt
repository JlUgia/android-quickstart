package com.google.android.gms.samples.pay.data.model

import org.json.JSONArray
import org.json.JSONObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class PunchCard(val jwtRepresentation: String){
    val id: String
    val programName: String
    val points: Int
    val activeOfferIds: List<String>

    init {
        val jwtPassBody = jwtRepresentation.split(".")[1]
        val passJson = JSONObject(String(Base64.UrlSafe.decode(jwtPassBody)))
            .getJSONObject("payload").getJSONArray("loyaltyObjects")
            .getJSONObject(0)

        // Initialize properties
        id = passJson.getString("id")
        points = passJson.getJSONObject("loyaltyPoints").getJSONObject("balance").getInt("int")
        programName = passJson
            .getJSONObject("classReference")
            .getJSONObject("localizedProgramName")
            .getJSONObject("defaultValue")
            .getString("value")

        // Parse offers
        val offerJsonArray = if (passJson.has("linkedOfferIds")) passJson.getJSONArray("linkedOfferIds") else JSONArray()
        val mutableList: MutableList<String> = mutableListOf()
        for (i in 0 until offerJsonArray.length()) {
            mutableList.add(offerJsonArray.getString(i))
        }

        activeOfferIds = mutableList.toList()
    }
}