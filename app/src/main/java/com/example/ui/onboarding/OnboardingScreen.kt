package com.example.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.AppLinks
import com.example.localization.LocalAppStrings
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AccentRedDark
import com.example.ui.theme.BgDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.SurfacePrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Videocam,
            title = strings.onboarding.title1,
            description = strings.onboarding.desc1
        ),
        OnboardingPage(
            icon = Icons.Default.DarkMode,
            title = strings.onboarding.title2,
            description = strings.onboarding.desc2
        ),
        OnboardingPage(
            icon = Icons.Default.Shield,
            title = strings.onboarding.title3,
            description = strings.onboarding.desc3
        )
    )

    var currentPage by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BgDark,
                        SurfacePrimary,
                        BgDark
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val page = pages[currentPage]

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentRed.copy(alpha = 0.25f),
                                AccentRedDark.copy(alpha = 0.08f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = page.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = page.description,
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // PAGE INDICATOR DOTS
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in pages.indices) {
                    val isActive = i == currentPage
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 24.dp else 8.dp, 8.dp)
                            .background(
                                color = if (isActive) AccentRed else BorderSubtle,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (currentPage < pages.lastIndex) {
                        currentPage++
                    } else {
                        onFinished()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (currentPage < pages.lastIndex) strings.onboarding.next else strings.onboarding.getStarted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (currentPage == pages.lastIndex) {
                Spacer(modifier = Modifier.height(16.dp))
                val context = LocalContext.current
                val annotatedText = remember(strings) {
                    buildAnnotatedString {
                        append(strings.onboarding.disclaimerPrefix)
                        pushStringAnnotation(tag = "TERMS", annotation = AppLinks.TERMS_AND_CONDITIONS_URL)
                        withStyle(
                            style = SpanStyle(
                                color = AccentRed,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(strings.onboarding.termsOfService)
                        }
                        pop()
                        append(strings.onboarding.andConnector)
                        pushStringAnnotation(tag = "PRIVACY", annotation = AppLinks.PRIVACY_POLICY_URL)
                        withStyle(
                            style = SpanStyle(
                                color = AccentRed,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(strings.privacyPolicy)
                        }
                        pop()
                        append(strings.onboarding.disclaimerSuffix)
                    }
                }

                @Suppress("DEPRECATION")
                androidx.compose.foundation.text.ClickableText(
                    text = annotatedText,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        annotatedText.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                    }
                )
            }
        }
    }
}
