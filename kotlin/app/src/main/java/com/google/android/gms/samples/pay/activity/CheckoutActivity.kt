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

package com.google.android.gms.samples.pay.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.contract.TaskResultContracts.GetPaymentDataResult
import com.google.android.gms.samples.pay.R
import com.google.android.gms.samples.pay.ui.ProductScreen
import com.google.android.gms.samples.pay.viewmodel.CheckoutViewModel
import com.google.android.gms.samples.pay.viewmodel.OrderState
import com.google.android.gms.samples.pay.viewmodel.PunchCardSelection
import com.google.android.gms.samples.pay.viewmodel.PunchCardState

class CheckoutActivity : ComponentActivity() {

    private val addToGoogleWalletRequestCode = 1000

    private val paymentDataLauncher = registerForActivityResult(GetPaymentDataResult()) { taskResult ->
        when (taskResult.status.statusCode) {
            CommonStatusCodes.SUCCESS -> {
                taskResult.result!!.let {
                    Log.i("Google Pay result:", it.toJson())
                    model.setPaymentData(it)
                }
            }
            //CommonStatusCodes.CANCELED -> The user canceled
            //AutoResolveHelper.RESULT_ERROR -> The API returned an error (it.status: Status)
            //CommonStatusCodes.INTERNAL_ERROR -> Handle other unexpected errors
        }
    }

    private val model: CheckoutViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val payState: OrderState by model.orderState.collectAsStateWithLifecycle()
            val punchCardState: PunchCardState by model.punchCardState.collectAsStateWithLifecycle()
            ProductScreen(
                title = "Chef's Soup",
                description = "A warm creamy dish with your favorite flavors and all the energy you need to navigate your days",
                price = "$19.20",
                image = R.drawable.food_soup,
                payUiState = payState,
                punchCardState = punchCardState,
                onPunchCardSelected = model::setPunchCardSelection,
                onGoogleWalletButtonClick = {
                    model.saveCurrentPass(this, addToGoogleWalletRequestCode)
                },
                onGooglePayButtonClick = this::requestPayment,
            )
        }
    }

    private fun requestPayment() {
        val task = model.getLoadPaymentDataTask(priceCents = 1000L)
        task.addOnCompleteListener(paymentDataLauncher::launch)
    }
}