package com.example.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SupportAgent
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.SUPPORT_PHONE_NUMBERS
import com.example.data.model.UserProfile
import com.example.ui.components.dialPhoneNumber
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.PureWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
  strings: AppStrings,
  userId: String,
  userEmail: String,
  userProfile: UserProfile?,
  isSaving: Boolean,
  onSaveProfile: (name: String, mobile: String, city: String, address: String, altPhone: String, onSuccess: () -> Unit) -> Unit,
  onToggleLanguage: () -> Unit,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  var name by remember(userProfile) { mutableStateOf(userProfile?.name ?: "") }
  var mobile by remember(userProfile) { mutableStateOf(userProfile?.mobileNumber ?: "") }
  var city by remember(userProfile) { mutableStateOf(userProfile?.city ?: "") }
  var address by remember(userProfile) { mutableStateOf(userProfile?.address ?: "") }
  var altPhone by remember(userProfile) { mutableStateOf(userProfile?.alternatePhone ?: "") }
  var feedback by remember { mutableStateOf<String?>(null) }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier =
        Modifier.widthIn(max = 640.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = strings.profileTitle,
        style = MaterialTheme.typography.headlineSmall,
        color = NavyDeep,
        fontWeight = FontWeight.Bold,
      )

      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          if (userEmail.isNotBlank()) {
            Text(
              text = userEmail,
              style = MaterialTheme.typography.labelLarge,
              color = EmeraldPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
          }
          Text(
            text = "${strings.yourAccountUid}: $userId",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(14.dp))

          OutlinedTextField(
            value = name,
            onValueChange = {
              name = it
              feedback = null
            },
            label = { Text(strings.fullNameLabel) },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
            shape = RoundedCornerShape(12.dp),
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = mobile,
            onValueChange = { input ->
              mobile = input.filter { it.isDigit() || it == '+' }.take(15)
              feedback = null
            },
            label = { Text(strings.mobileLabel) },
            leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().testTag("profile_mobile_input"),
            shape = RoundedCornerShape(12.dp),
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = address,
            onValueChange = {
              address = it
              feedback = null
            },
            label = { Text(strings.addressLabel) },
            leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag("profile_address_input"),
            shape = RoundedCornerShape(12.dp),
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = city,
            onValueChange = {
              city = it
              feedback = null
            },
            label = { Text(strings.cityLabel) },
            leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("profile_city_input"),
            shape = RoundedCornerShape(12.dp),
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = altPhone,
            onValueChange = { input ->
              altPhone = input.filter { it.isDigit() || it == '+' }.take(15)
              feedback = null
            },
            label = { Text(strings.altPhoneLabel) },
            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().testTag("profile_alt_phone_input"),
            shape = RoundedCornerShape(12.dp),
          )

          if (feedback != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(color = EmeraldLightContainer, shape = RoundedCornerShape(10.dp)) {
              Text(
                text = feedback ?: "",
                color = EmeraldOnContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          Button(
            onClick = {
              if (name.trim().length < 2) {
                feedback = strings.invalidNameError
              } else if (mobile.filter { it.isDigit() }.length < 10) {
                feedback = strings.invalidMobileError
              } else {
                onSaveProfile(name.trim(), mobile.trim(), city.trim(), address.trim(), altPhone.trim()) {
                  feedback = strings.profileSavedToast
                }
              }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("profile_save_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
          ) {
            if (isSaving) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = PureWhite,
                strokeWidth = 2.dp,
              )
            } else {
              Text(strings.updateProfileBtn, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedButton(
            onClick = onToggleLanguage,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("profile_language_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Filled.Translate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("${strings.languageButtonLabel} (Hindi / English)")
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = NavyDeep)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = strings.supportPhoneLabel,
              style = MaterialTheme.typography.titleSmall,
              color = NavyDeep,
              fontWeight = FontWeight.Bold,
            )
          }

          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            SUPPORT_PHONE_NUMBERS.forEach { num ->
              Button(
                onClick = { dialPhoneNumber(context, num) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
              ) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(num, fontWeight = FontWeight.Bold)
              }
            }
          }

          OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
          ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.signOut, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
