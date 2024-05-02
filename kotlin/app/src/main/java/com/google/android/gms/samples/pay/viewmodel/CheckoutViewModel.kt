/*
 * Copyright 2024 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.samples.pay.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.result.getOrNull
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.pay.Pay
import com.google.android.gms.pay.PayClient
import com.google.android.gms.samples.pay.data.model.PunchCard
import com.google.android.gms.samples.pay.data.repository.PunchCardRepository
import com.google.android.gms.samples.pay.util.PaymentsUtil
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.io.encoding.ExperimentalEncodingApi

class CheckoutViewModel(application: Application) : AndroidViewModel(application) {

    private val _orderState: MutableStateFlow<OrderState> =
        MutableStateFlow(OrderState.NotStarted)
    val orderState: StateFlow<OrderState> = _orderState.asStateFlow()

    private val _punchCardState: MutableStateFlow<PunchCardState> =
        MutableStateFlow(PunchCardState.Fetching)
    val punchCardState: StateFlow<PunchCardState> = _punchCardState.asStateFlow()

    private var punchCardSelection: PunchCardSelection = PunchCardSelection.None
    private val punchCardRepository: PunchCardRepository = PunchCardRepository()

    // A client for interacting with the Google Pay API
    private val paymentsClient: PaymentsClient = PaymentsUtil.createPaymentsClient(application)

    // A client for interacting with the Google Wallet API
    private val walletClient: PayClient = Pay.getClient(application)

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private lateinit var userId: String

    init {
        viewModelScope.launch {
            verifyGooglePayReadiness()
        }
    }

    fun setUserIdAndDeterminePunchCardSelection(userAccountName: String) {
        userId = userAccountName.replace("[@.]".toRegex(), "") + "_test"

        // Fetch active punch card and change selection if it exists
        viewModelScope.launch {
            setPunchCardSelection(selection = fetchActivePunchCard()?.let {
                PunchCardSelection.Existing(it)
            } ?: PunchCardSelection.New)
        }
    }

    /**
     * Determine the user's ability to pay with a payment method supported by your app and display
     * a Google Pay payment button.
    ) */
    private suspend fun verifyGooglePayReadiness() {
        val newUiState: OrderState = try {
            if (fetchCanUseGooglePay()) {
                OrderState.Available
            } else {
                OrderState.Error(CommonStatusCodes.ERROR)
            }
        } catch (exception: ApiException) {
            OrderState.Error(exception.statusCode, exception.message)
        }

        _orderState.update { newUiState }
    }

    /**
     * Determine the user's ability to pay with a payment method supported by your app.
    ) */
    private suspend fun fetchCanUseGooglePay(): Boolean {
        val request = IsReadyToPayRequest.fromJson(PaymentsUtil.isReadyToPayRequest().toString())
        return paymentsClient.isReadyToPay(request).await()
    }

    private suspend fun fetchActivePunchCard(): PunchCard? = withContext(Dispatchers.IO) {
        punchCardRepository.getActiveCard(userId)
            .third.getOrNull()
            ?.let(::PunchCard)
            ?.takeIf {
                it.id.endsWith(userId)
            }
    }

    private suspend fun updateOrCreatePunchCard() {
        _punchCardState.update { PunchCardState.Updating }

        val newCardState = withContext(Dispatchers.IO) {
            punchCardRepository.addPointToCard(userId)
                .third.getOrNull()
                ?.let(::PunchCard)
                }?.let(PunchCardState::Updated) ?: PunchCardState.None

        _punchCardState.update { newCardState }
    }

    /**
     * Creates a [Task] that starts the payment process with the transaction details included.
     *
     * @return a [Task] with the payment information.
     * @see [PaymentDataRequest](https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentsClient#loadPaymentData(com.google.android.gms.wallet.PaymentDataRequest)
    ) */
    fun getLoadPaymentDataTask(priceCents: Long): Task<PaymentData> {
        val paymentDataRequestJson = PaymentsUtil.getPaymentDataRequest(priceCents)
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())
        return paymentsClient.loadPaymentData(request)
    }

    /**
     * At this stage, the user has already seen a popup informing them an error occurred. Normally,
     * only logging is required.
     *
     * @param statusCode will hold the value of any constant from CommonStatusCode or one of the
     * WalletConstants.ERROR_CODE_* constants.
     * @see [
     * Wallet Constants Library](https://developers.google.com/android/reference/com/google/android/gms/wallet/WalletConstants.constant-summary)
     */
    private fun handleError(statusCode: Int, message: String?) {
        Log.e("Google Pay API error", "Error code: $statusCode, Message: $message")
    }

    fun setPaymentData(paymentData: PaymentData) {
        val payState = extractPaymentBillingName(paymentData)?.let {

            // Update punch card
            if (punchCardSelection !is PunchCardSelection.None) {
                viewModelScope.launch {
                    updateOrCreatePunchCard()
                }
            }

            OrderState.PaymentCompleted(payerName = it, punchCardSelection = punchCardSelection)

        } ?: OrderState.Error(CommonStatusCodes.INTERNAL_ERROR)

        _orderState.update { payState }
    }

    fun setPunchCardSelection(selection: PunchCardSelection) {
        punchCardSelection = selection
        _punchCardState.update { PunchCardState.Active(selection) }
    }

    private fun extractPaymentBillingName(paymentData: PaymentData): String? {
        val paymentInformation = paymentData.toJson()

        try {
            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
            val paymentMethodData =
                JSONObject(paymentInformation).getJSONObject("paymentMethodData")
            val billingName = paymentMethodData.getJSONObject("info")
                .getJSONObject("billingAddress").getString("name")
            Log.d("BillingName", billingName)

            // Logging token string.
            Log.d(
                "Google Pay token", paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("token")
            )

            return billingName
        } catch (error: JSONException) {
            Log.e("handlePaymentSuccess", "Error: $error")
        }

        return null
    }

    /**
     * Exposes the `savePassesJwt` method in the wallet client
     */
    fun saveCurrentPass(passJwt: String, activity: Activity, requestCode: Int) {
        walletClient.savePassesJwt(passJwt, activity, requestCode)
    }
}

abstract class OrderState internal constructor() {
    object NotStarted : OrderState()
    object Available : OrderState()
    class PaymentCompleted(val payerName: String, val punchCardSelection: PunchCardSelection) : OrderState()
    class Error(val code: Int, val message: String? = null) : OrderState()
}

abstract class PunchCardState internal constructor() {
    object None : PunchCardState()
    object Fetching : PunchCardState()
    class Active(val selection: PunchCardSelection) : PunchCardState()
    object Updating : PunchCardState()
    class Updated(val punchCard: PunchCard) : PunchCardState()

    fun cardIsComplete() = this is Updated && punchCard.points == 0
}

sealed class PunchCardSelection {
    object None : PunchCardSelection()
    object New : PunchCardSelection()
    class Existing(val punchCard: PunchCard) : PunchCardSelection()
}

suspend fun <T> Task<T>.awaitTask(cancellationTokenSource: CancellationTokenSource? = null): Task<T> {
    return if (isComplete) this else suspendCancellableCoroutine { cont ->
        // Run the callback directly to avoid unnecessarily scheduling on the main thread.
        addOnCompleteListener(DirectExecutor, cont::resume)

        cancellationTokenSource?.let { cancellationSource ->
            cont.invokeOnCancellation { cancellationSource.cancel() }
        }
    }
}

/**
 * An [Executor] that just directly executes the [Runnable].
 */
private object DirectExecutor : Executor {
    override fun execute(r: Runnable) {
        r.run()
    }
}
