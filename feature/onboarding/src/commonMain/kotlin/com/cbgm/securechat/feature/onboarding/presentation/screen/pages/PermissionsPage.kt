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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.onboarding.presentation.screen.pages.component.ListingRow


@Composable
fun PermissionsPage(onRequestPermissions: () -> Unit) {
    Column(
        Modifier.padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Permissions",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(MaterialTheme.spacing.base))
        Text(
            text = "SecureChat asks only for features you choose to use.",
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .74f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        ListingRow(
            index = "01",
            title = "Notifications",
            description = "Receive new-message alerts."
        )
        ListingRow(
            index = "02",
            title = "Contacts",
            description = "Find existing phone-book contacts."
        )
        ListingRow(
            index = "03",
            title = "Camera",
            description = "Scan SecureChat identity QR codes."
        )
        ListingRow(
            index = "04",
            title = "Phone number",
            description = "Try to fill your SIM number automatically without opening a picker."
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        SecureChatApprovalButton(
            onClick = onRequestPermissions,
            text = "Allow and continue"
        )
        Spacer(Modifier.height(MaterialTheme.spacing.base))
        Text(
            text = "Denied permissions can be enabled later in system settings.",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = .58f),
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
fun PermissionsPagePreview(){
    SecureChatTheme {
        PermissionsPage {  }
    }
}
