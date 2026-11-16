package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.PureWhite

@Composable
fun AuthScreen(
  strings: AppStrings,
  onToggleLanguage: () -> Unit,
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(NavyDeep, NavySecondary, Color(0xFFF3F7FA)),
            startY = 0f,
            endY = 1200f,
          )
        )
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
        // Top Header + Language Switch
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier =
                Modifier.size(44.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(EmeraldPrimary),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.LocalShipping,
                contentDescription = strings.brandTitle,
                tint = PureWhite,
                modifier = Modifier.size(26.dp),
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = strings.brandTitle,
                style = MaterialTheme.typography.titleLarge,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = strings.allIndiaServiceBadge,
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldLightContainer,
              )
            }
          }

          OutlinedButton(
            onClick = onToggleLanguage,
            modifier = Modifier.testTag("auth_language_toggle"),
            shape = RoundedCornerShape(20.dp),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor = PureWhite.copy(alpha = 0.14f),
                contentColor = PureWhite,
              ),
          ) {
            Icon(
              imageVector = Icons.Filled.Translate,
              contentDescription = strings.languageButtonLabel,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = strings.languageButtonLabel, style = MaterialTheme.typography.labelMedium)
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Banner Image
        Card(
          shape = RoundedCornerShape(20.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
          modifier = Modifier.fillMaxWidth().height(200.dp),
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            Image(
              painter = painterResource(id = R.drawable.img_transport_hero),
              contentDescription = strings.brandTitle,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      colors = listOf(Color.Transparent, NavyDeep.copy(alpha = 0.78f))
                    )
                  )
            )
            Column(
              modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
              Surface(
                color = EmeraldPrimary,
                shape = RoundedCornerShape(8.dp),
              ) {
                Text(
                  text = "PAN-INDIA FLEET",
                  style = MaterialTheme.typography.labelSmall,
                  color = PureWhite,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = strings.brandSubtitle,
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sign-In Card
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(24.dp)) {
            Text(
              text = strings.welcomeHeadline,
              style = MaterialTheme.typography.headlineSmall,
              color = NavyDeep,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = strings.welcomeSubhead,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(18.dp))

            listOf(strings.authBenefit1, strings.authBenefit2, strings.authBenefit3).forEach {
              benefit ->
              Row(
                modifier = Modifier.padding(vertical = 5.dp),
                verticalAlignment = Alignment.Top,
              ) {
                Icon(
                  imageVector = Icons.Filled.CheckCircle,
                  contentDescription = null,
                  tint = EmeraldPrimary,
                  modifier = Modifier.size(20.dp).padding(top = 2.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = benefit,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }

            if (errorMessage != null) {
              Spacer(modifier = Modifier.height(14.dp))
              Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = errorMessage ?: "",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.padding(12.dp),
                )
              }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
              onClick = {
                errorMessage = null
                isLoading = true
                onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                  },
                  onAuthError = { err ->
                    isLoading = false
                    errorMessage = err
                  },
                  scope = scope,
                  onAuthCancelled = { isLoading = false },
                )
              },
              enabled = !isLoading,
              modifier =
                Modifier.fillMaxWidth().height(56.dp).testTag("google_sign_in_button"),
              shape = RoundedCornerShape(16.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = EmeraldPrimary,
                  contentColor = PureWhite,
                ),
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = PureWhite,
                  strokeWidth = 2.5.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = strings.signingIn, style = MaterialTheme.typography.titleMedium)
              } else {
                Box(
                  modifier = Modifier.size(28.dp).clip(CircleShape).background(PureWhite),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = "G",
                    color = NavyDeep,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleSmall,
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = strings.signInWithGoogle,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }
  }
}
