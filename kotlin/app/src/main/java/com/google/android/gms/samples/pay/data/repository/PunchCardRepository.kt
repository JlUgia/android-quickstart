package com.google.android.gms.samples.pay.data.repository

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.ResponseOf
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.awaitResponse
import com.github.kittinunf.fuel.core.awaitResult
import com.github.kittinunf.result.Result
import com.google.gson.Gson

class PunchCardRepository {

    init {
        FuelManager.instance.basePath = "https://punchcard-bssqzccpya-uw.a.run.app"
    }

    fun getActiveCard(userId: String): ResponseResultOf<String> {
        return Fuel.get("/punchcard/$userId").responseString()
    }

    fun addPointToCard(userId: String): ResponseResultOf<String> {
        return Fuel.post("/punchcard/$userId").responseString()
    }
}