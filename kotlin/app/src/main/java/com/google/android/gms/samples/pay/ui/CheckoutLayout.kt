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

package com.google.android.gms.samples.pay.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.samples.pay.R
import com.google.android.gms.samples.pay.util.PaymentsUtil
import com.google.android.gms.samples.pay.viewmodel.OrderState
import com.google.android.gms.samples.pay.viewmodel.PunchCardSelection
import com.google.android.gms.samples.pay.viewmodel.PunchCardState
import com.google.pay.button.PayButton
import com.google.wallet.button.WalletButton
import java.lang.IllegalStateException

@Composable
fun ProductScreen(
    title: String,
    description: String,
    priceCents: Long,
    discountCents: Long?,
    image: Int,
    onGooglePayButtonClick: () -> Unit,
    punchCardState: PunchCardState,
    onPunchCardSelected: (selection: PunchCardSelection) -> Unit,
    onGoogleWalletButtonClick: (passJwt: String) -> Unit,
    payUiState: OrderState,
) {
    val padding = 40.dp
    val black = Color(0xff000000.toInt())
    val grey = Color(0xffeeeeee.toInt())
    val discountGreen = Color(0xff008800.toInt())

    if (payUiState is OrderState.PaymentCompleted) {
        Column(
            modifier = Modifier
                .testTag("successScreen")
                .background(grey)
                .padding(padding)
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                contentDescription = null,
                painter = painterResource(R.drawable.check_circle),
                colorFilter = ColorFilter.tint(Color.LightGray),
                modifier = Modifier
                    .width(200.dp)
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Your food is on the way!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            if (payUiState.punchCardSelection !is PunchCardSelection.None) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val loyaltyTitle = if (punchCardState.cardIsComplete()) {
                        "You have completed your punch card!"
                    } else {
                        "You have accumulated one point!"
                    }
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = loyaltyTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Start
                    )

                    val creatingNewCard = payUiState.punchCardSelection is PunchCardSelection.New
                    val punchCardDetailText = when (punchCardState) {
                        is PunchCardState.Updating -> {
                            if (creatingNewCard) {
                                "We are creating your new punch card. Collect points with every order and earn a reward when you complete the card!"
                            } else {
                                "We are updating your punch card..."
                            }
                        }
                        is PunchCardState.Updated -> {
                            if (creatingNewCard) {
                                "Add your new punch card to Google Wallet to keep track of your progress, and make new orders to collect more points!"
                            } else {
                                if (punchCardState.cardIsComplete()) {
                                    "Your card will be reset and you'll enjoy a discount for your next order!\nContinue to track your progress on Google Wallet"
                                } else {
                                    val remainingPoints = 10 - punchCardState.punchCard.points
                                    "You are $remainingPoints point${if (remainingPoints > 1) "s" else ""} away from your next reward. Check your pass on Google Wallet to keep track of your progress"
                                }
                            }
                        }
                        else -> throw IllegalStateException()
                    }

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = punchCardDetailText,
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    when (punchCardState) {
                        is PunchCardState.Updating -> CircularProgressIndicator(
                            strokeWidth = 1.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        is PunchCardState.Updated ->
                            WalletButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onGoogleWalletButtonClick(punchCardState.punchCard.jwtRepresentation)
                                },
                            )
                    }
                }
            }
        }

    } else {
        Column(
            modifier = Modifier
                .background(grey)
                .padding(padding)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(space = padding / 2),
        ) {
            Image(
                contentDescription = null,
                painter = painterResource(image),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
            Column {
                Text(
                    text = title, color = black, fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                Text(text = String.format("$%.2f", priceCents.toDouble() / 100), color = black)
                if (discountCents != null) {
                    Text(
                        text = "(includes $${String.format("%.2f", discountCents.toDouble() / 100)} punch-card discount)",
                        fontSize = 12.sp,
                        color = discountGreen
                    )
                }
            }
            Text(
                text = "Description", color = black, fontWeight = FontWeight.Bold
            )
            Text(
                text = description, color = black
            )
            Column {
                Text(
                    modifier = Modifier.padding(0.dp),
                    text = "Your punch card lets you accumulate points:",
                    color = black
                )
                PunchCardSelector(
                    state = punchCardState,
                    onPunchCardSelected = onPunchCardSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (payUiState !is OrderState.NotStarted) {
                PayButton(
                    modifier = Modifier
                        .testTag("payButton")
                        .fillMaxWidth(),
                    onClick = onGooglePayButtonClick,
                    allowedPaymentMethods = PaymentsUtil.allowedPaymentMethods.toString()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PunchCardSelector(
    modifier: Modifier,
    state: PunchCardState,
    onPunchCardSelected: (selection: PunchCardSelection) -> Unit,
) {

    val hasExistingCard = state is PunchCardState.Active && state.selection is PunchCardSelection.Existing
    val hasNoCard = state is PunchCardState.Active && state.selection is PunchCardSelection.None

    val label: String = when (state) {
        is PunchCardState.Fetching -> "Loading..."
        is PunchCardState.Active -> when (state.selection) {
            is PunchCardSelection.Existing -> state.selection.punchCard.programName
            else -> if (state.selection is PunchCardSelection.None) "Create a punch card and get free meals" else "A new punch card will be created"
        }

        else -> throw RuntimeException()
    }

    FilterChip(
        modifier = modifier,
        enabled = state !is PunchCardState.Fetching,
        selected = hasExistingCard,
        onClick = {
            if (!hasExistingCard) {
                onPunchCardSelected(if (hasNoCard) PunchCardSelection.New else PunchCardSelection.None)
            }
        },
        label = { Text(label) },
        leadingIcon = {
            if (state is PunchCardState.Fetching) {
                CircularProgressIndicator(
                    strokeWidth = 1.dp,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            } else if (state is PunchCardState.Active) {
                val icon = when (state.selection) {
                    is PunchCardSelection.None -> Icons.Filled.Add
                    else -> Icons.Filled.Done
                }
                Icon(
                    icon,
                    contentDescription = "Localized description",
                    Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        }
    )
}