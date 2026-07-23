package com.cbgm.securechat.feature.onboarding.presentation.screen.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.onboarding.presentation.screen.pages.component.ListingRow

@Composable
fun PrivacyPage(onNext: () -> Unit) {
    Column(
        Modifier.padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Your privacy comes first",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        ListingRow(
            index = "01",
            title = "End-to-end encryption",
            description = "Messages are encrypted automatically when both identities are available.",
        )
        ListingRow(
            index = "02",
            title = "Your contacts stay local",
            description = "Contacts are used to match people by phone number on your device.",
        )
        ListingRow(
            index = "03",
            title = "Your identity belongs to you",
            description = "Private identity keys remain protected on this device.",
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        SecureChatApprovalButton(
            onClick = onNext,
            text = "Continue",
        )
    }
}

@Preview
@Composable
private fun PrivacyPagePreview() {
    SecureChatTheme {
        PrivacyPage {}
    }
}
