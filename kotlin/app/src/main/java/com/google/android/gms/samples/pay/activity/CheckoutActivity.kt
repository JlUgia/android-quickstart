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

import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

    private val fixedPriceCents = 1950L

    private val accountChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val userId = it.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) ?: "test_user_id"
        model.setUserIdAndDeterminePunchCardSelection(userId)
    }

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
        fetchUserAccountOrLaunchChooser()

        setContent {
            val payState: OrderState by model.orderState.collectAsStateWithLifecycle()
            val punchCardState: PunchCardState by model.punchCardState.collectAsStateWithLifecycle()

            val state = punchCardState
            val hasActiveOffer = state is PunchCardState.Active && state.selection is PunchCardSelection.Existing && state.selection.punchCard.activeOfferIds.isNotEmpty()
            val discountCents = if (hasActiveOffer) 600L else null
            val finalPrice = fixedPriceCents - (discountCents ?: 0)

            ProductScreen(
                title = "Chef's Soup",
                description = "A warm creamy dish with your favorite flavors and all the energy you need to navigate your days",
                priceCents = finalPrice,
                discountCents = discountCents,
                image = R.drawable.food_soup,
                payUiState = payState,
                punchCardState = punchCardState,
                onPunchCardSelected = model::setPunchCardSelection,
                onGoogleWalletButtonClick = {
                    model.saveCurrentPass(it, this, addToGoogleWalletRequestCode)
                },
                onGooglePayButtonClick = {
                    requestPayment(finalPrice)
                },
            )
        }
    }

    private fun requestPayment(priceCents: Long) {
        val task = model.getLoadPaymentDataTask(priceCents)
        task.addOnCompleteListener(paymentDataLauncher::launch)
    }

    private fun fetchUserAccountOrLaunchChooser() {
        val userAccounts = AccountManager.get(this).getAccountsByType("com.google")
        if (userAccounts.isEmpty()) {
            val intent = AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null)
            accountChooserLauncher.launch(intent)
        } else {
            model.setUserIdAndDeterminePunchCardSelection(userAccounts.first().name)
        }
    }
}