package com.example.ui.auth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.PureWhite

@Composable
fun ProfileRegistrationScreen(
  strings: AppStrings,
  initialName: String,
  userEmail: String,
  isSaving: Boolean,
  errorMessage: String?,
  onToggleLanguage: () -> Unit,
  onSignOut: () -> Unit,
  onSaveProfile: (name: String, mobile: String, city: String, address: String, altPhone: String) -> Unit,
) {
  var name by remember(initialName) { mutableStateOf(initialName) }
  var mobile by remember { mutableStateOf("") }
  var city by remember { mutableStateOf("") }
  var address by remember { mutableStateOf("") }
  var altPhone by remember { mutableStateOf("") }
  var validationError by remember { mutableStateOf<String?>(null) }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(NavyDeep)
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.brandTitle,
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          OutlinedButton(
            onClick = onToggleLanguage,
            shape = RoundedCornerShape(20.dp),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor = PureWhite.copy(alpha = 0.14f),
                contentColor = PureWhite,
              ),
          ) {
            Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.languageButtonLabel)
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(22.dp)) {
            Text(
              text = strings.mobileRegTitle,
              style = MaterialTheme.typography.headlineSmall,
              color = NavyDeep,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = strings.mobileRegSubtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (userEmail.isNotBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = userEmail,
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldPrimary,
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
              value = name,
              onValueChange = {
                name = it
                validationError = null
              },
              label = { Text(strings.fullNameLabel) },
              placeholder = { Text(strings.fullNamePlaceholder) },
              leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("reg_name_input"),
              shape = RoundedCornerShape(14.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
              value = mobile,
              onValueChange = { input ->
                mobile = input.filter { it.isDigit() || it == '+' }.take(15)
                validationError = null
              },
              label = { Text(strings.mobileLabel) },
              placeholder = { Text(strings.mobilePlaceholder) },
              leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null) },
              prefix = { Text("+91 ") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              modifier = Modifier.fillMaxWidth().testTag("reg_mobile_input"),
              shape = RoundedCornerShape(14.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
              value = address,
              onValueChange = { address = it },
              label = { Text(strings.addressLabel) },
              placeholder = { Text(strings.addressPlaceholder) },
              leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null) },
              modifier = Modifier.fillMaxWidth().testTag("reg_address_input"),
              shape = RoundedCornerShape(14.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
              value = city,
              onValueChange = { city = it },
              label = { Text(strings.cityLabel) },
              placeholder = { Text(strings.cityPlaceholder) },
              leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = null) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("reg_city_input"),
              shape = RoundedCornerShape(14.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
              value = altPhone,
              onValueChange = { altPhone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
              label = { Text(strings.altPhoneLabel) },
              placeholder = { Text(strings.altPhonePlaceholder) },
              leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              modifier = Modifier.fillMaxWidth().testTag("reg_alt_phone_input"),
              shape = RoundedCornerShape(14.dp),
            )

            val shownError = validationError ?: errorMessage
            if (shownError != null) {
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = shownError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
              )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = {
                val digits = mobile.filter { it.isDigit() }
                when {
                  name.trim().length < 2 -> validationError = strings.invalidNameError
                  digits.length < 10 -> validationError = strings.invalidMobileError
                  else -> onSaveProfile(name.trim(), mobile.trim(), city.trim(), address.trim(), altPhone.trim())
                }
              },
              enabled = !isSaving,
              modifier = Modifier.fillMaxWidth().height(56.dp).testTag("reg_submit_button"),
              shape = RoundedCornerShape(16.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = EmeraldPrimary,
                  contentColor = PureWhite,
                ),
            ) {
              if (isSaving) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = PureWhite,
                  strokeWidth = 2.dp,
                )
              } else {
                Text(
                  text = strings.saveAndContinue,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
              onClick = onSignOut,
              modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
              Text(text = strings.signOut, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }
  }
}
