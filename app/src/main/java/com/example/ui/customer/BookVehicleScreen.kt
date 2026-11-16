package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.UserProfile
import com.example.data.model.VehicleType
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.PopularIndianTransportCities
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyContainer
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyOnContainer
import com.example.ui.theme.PureWhite
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookVehicleScreen(
  strings: AppStrings,
  userProfile: UserProfile?,
  isSubmitting: Boolean,
  errorMessage: String?,
  onSubmitBooking: (Booking, onSuccess: () -> Unit) -> Unit,
  onNavigateToMyBookings: () -> Unit,
) {
  var customerName by remember(userProfile) { mutableStateOf(userProfile?.name ?: "") }
  var customerPhone by remember(userProfile) { mutableStateOf(userProfile?.mobileNumber ?: "") }
  var pickupLocation by remember { mutableStateOf("") }
  var deliveryLocation by remember { mutableStateOf("") }
  var selectedVehicle by remember { mutableStateOf(VehicleType.TATA_ACE) }
  var goodsDescription by remember { mutableStateOf("") }
  var goodsWeight by remember { mutableStateOf("") }
  var weightUnitTons by remember { mutableStateOf(true) }
  var specialInstructions by remember { mutableStateOf("") }

  // Default booking schedule: tomorrow at 10:00 AM
  val defaultCal = remember {
    Calendar.getInstance().apply {
      add(Calendar.DAY_OF_YEAR, 1)
      set(Calendar.HOUR_OF_DAY, 10)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
    }
  }
  var selectedDateMillis by remember { mutableLongStateOf(defaultCal.timeInMillis) }
  var selectedHour by remember { mutableIntStateOf(defaultCal.get(Calendar.HOUR_OF_DAY)) }
  var selectedMinute by remember { mutableIntStateOf(defaultCal.get(Calendar.MINUTE)) }

  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }
  var formError by remember { mutableStateOf<String?>(null) }
  var showSuccessDialog by remember { mutableStateOf(false) }

  val formattedDate =
    remember(selectedDateMillis) {
      SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
    }
  val formattedTime =
    remember(selectedHour, selectedMinute) {
      val cal =
        Calendar.getInstance().apply {
          set(Calendar.HOUR_OF_DAY, selectedHour)
          set(Calendar.MINUTE, selectedMinute)
        }
      SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
    }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier =
        Modifier.widthIn(max = 720.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Hero Banner Card
      Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth().height(150.dp),
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
                  Brush.horizontalGradient(
                    colors = listOf(NavyDeep.copy(alpha = 0.88f), NavyDeep.copy(alpha = 0.35f))
                  )
                )
          )
          Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(color = EmeraldPrimary, shape = RoundedCornerShape(8.dp)) {
              Text(
                text = strings.allIndiaServiceBadge,
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
            Text(
              text = strings.bookingFormTitle,
              style = MaterialTheme.typography.headlineSmall,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = strings.brandSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.9f),
            )
          }
        }
      }

      // Admin Approval Info Banner
      Surface(
        color = NavyContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = NavyDeep,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = strings.bookingApprovalNote,
            style = MaterialTheme.typography.bodySmall,
            color = NavyOnContainer,
            fontWeight = FontWeight.Medium,
          )
        }
      }

      // Section 1: Customer Contact
      SectionCard(title = strings.sectionContact) {
        OutlinedTextField(
          value = customerName,
          onValueChange = {
            customerName = it
            formError = null
          },
          label = { Text(strings.fullNameLabel) },
          placeholder = { Text(strings.fullNamePlaceholder) },
          leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("booking_customer_name"),
          shape = RoundedCornerShape(12.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = customerPhone,
          onValueChange = { input ->
            customerPhone = input.filter { it.isDigit() || it == '+' }.take(15)
            formError = null
          },
          label = { Text(strings.mobileLabel) },
          placeholder = { Text(strings.mobilePlaceholder) },
          leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth().testTag("booking_customer_phone"),
          shape = RoundedCornerShape(12.dp),
        )
      }

      // Section 2: Pickup & Delivery Route
      SectionCard(title = strings.sectionRoute) {
        OutlinedTextField(
          value = pickupLocation,
          onValueChange = {
            pickupLocation = it
            formError = null
          },
          label = { Text(strings.pickupLocationLabel) },
          placeholder = { Text(strings.pickupPlaceholder) },
          leadingIcon = {
            Icon(Icons.Filled.MyLocation, contentDescription = null, tint = EmeraldPrimary)
          },
          modifier = Modifier.fillMaxWidth().testTag("booking_pickup_input"),
          shape = RoundedCornerShape(12.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = strings.popularCitiesHint,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PopularIndianTransportCities.forEach { city ->
            FilterChip(
              selected = pickupLocation.startsWith(city, ignoreCase = true),
              onClick = {
                pickupLocation = city
                formError = null
              },
              label = { Text(city, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = deliveryLocation,
          onValueChange = {
            deliveryLocation = it
            formError = null
          },
          label = { Text(strings.deliveryLocationLabel) },
          placeholder = { Text(strings.deliveryPlaceholder) },
          leadingIcon = {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = NavyDeep)
          },
          modifier = Modifier.fillMaxWidth().testTag("booking_delivery_input"),
          shape = RoundedCornerShape(12.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PopularIndianTransportCities.forEach { city ->
            FilterChip(
              selected = deliveryLocation.startsWith(city, ignoreCase = true),
              onClick = {
                deliveryLocation = city
                formError = null
              },
              label = { Text(city, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
      }

      // Section 3: Booking Date & Time
      SectionCard(title = strings.sectionSchedule) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.weight(1f).height(56.dp).testTag("booking_date_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = strings.selectDate, tint = NavyDeep)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = strings.selectDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = formattedDate,
                style = MaterialTheme.typography.titleSmall,
                color = NavyDeep,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.weight(1f).height(56.dp).testTag("booking_time_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            Icon(Icons.Filled.AccessTime, contentDescription = strings.selectTime, tint = EmeraldPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = strings.selectTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = formattedTime,
                style = MaterialTheme.typography.titleSmall,
                color = NavyDeep,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      // Section 4: All 14 Vehicle Types
      SectionCard(title = strings.sectionVehicleType) {
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          maxItemsInEachRow = 2,
        ) {
          VehicleType.entries.forEach { vehicle ->
            val isSelected = selectedVehicle == vehicle
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (isSelected) EmeraldLightContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              border =
                BorderStroke(
                  width = if (isSelected) 2.dp else 1.dp,
                  color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                ),
              modifier =
                Modifier.weight(1f, fill = true)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { selectedVehicle = vehicle }
                  .testTag("vehicle_type_${vehicle.name.lowercase()}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Filled.LocalShipping,
                  contentDescription = null,
                  tint = if (isSelected) EmeraldPrimary else NavyDeep,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (strings.isHindi) vehicle.labelHi else vehicle.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) EmeraldOnContainer else NavyDeep,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = if (strings.isHindi) vehicle.capacityHintHi else vehicle.capacityHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }

      // Section 5: Goods Description, Weight & Instructions
      SectionCard(title = strings.sectionGoodsDetails) {
        OutlinedTextField(
          value = goodsDescription,
          onValueChange = {
            goodsDescription = it
            formError = null
          },
          label = { Text(strings.goodsDescLabel) },
          placeholder = { Text(strings.goodsDescPlaceholder) },
          leadingIcon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
          modifier = Modifier.fillMaxWidth().testTag("booking_goods_desc"),
          shape = RoundedCornerShape(12.dp),
          minLines = 2,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = goodsWeight,
            onValueChange = {
              goodsWeight = it
              formError = null
            },
            label = { Text(strings.goodsWeightLabel) },
            placeholder = { Text(strings.goodsWeightPlaceholder) },
            leadingIcon = { Icon(Icons.Filled.Scale, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("booking_goods_weight"),
            shape = RoundedCornerShape(12.dp),
          )
          FilterChip(
            selected = !weightUnitTons,
            onClick = { weightUnitTons = false },
            label = { Text(strings.unitKg) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = NavyDeep,
                selectedLabelColor = PureWhite,
              ),
          )
          FilterChip(
            selected = weightUnitTons,
            onClick = { weightUnitTons = true },
            label = { Text(strings.unitTons) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = NavyDeep,
                selectedLabelColor = PureWhite,
              ),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = specialInstructions,
          onValueChange = { specialInstructions = it },
          label = { Text(strings.specialInstructionsLabel) },
          placeholder = { Text(strings.specialInstructionsPlaceholder) },
          leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) },
          modifier = Modifier.fillMaxWidth().testTag("booking_instructions"),
          shape = RoundedCornerShape(12.dp),
          minLines = 2,
        )
      }

      val shownError = formError ?: errorMessage
      if (shownError != null) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = shownError,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(14.dp),
          )
        }
      }

      // Large BOOK NOW Button
      Button(
        onClick = {
          val digits = customerPhone.filter { it.isDigit() }
          when {
            customerName.trim().length < 2 ||
              digits.length < 10 ||
              pickupLocation.trim().length < 2 ||
              deliveryLocation.trim().length < 2 ||
              goodsDescription.trim().length < 2 ||
              goodsWeight.trim().isEmpty() -> {
              formError = strings.requiredFieldError
            }
            else -> {
              val combinedCal =
                Calendar.getInstance().apply {
                  timeInMillis = selectedDateMillis
                  set(Calendar.HOUR_OF_DAY, selectedHour)
                  set(Calendar.MINUTE, selectedMinute)
                }
              val unitLabel = if (weightUnitTons) "Tons" else "Kg"
              val weightFormatted = "${goodsWeight.trim()} $unitLabel"
              val booking =
                Booking(
                  customerName = customerName.trim(),
                  customerPhone = customerPhone.trim(),
                  pickupLocation = pickupLocation.trim(),
                  deliveryLocation = deliveryLocation.trim(),
                  bookingDateTimeMillis = combinedCal.timeInMillis,
                  bookingDateLabel = formattedDate,
                  bookingTimeLabel = formattedTime,
                  vehicleType = selectedVehicle.label,
                  goodsDescription = goodsDescription.trim(),
                  goodsWeight = weightFormatted,
                  specialInstructions = specialInstructions.trim(),
                  status = BookingStatus.NEW_BOOKING.label,
                )
              onSubmitBooking(booking) {
                pickupLocation = ""
                deliveryLocation = ""
                goodsDescription = ""
                goodsWeight = ""
                specialInstructions = ""
                showSuccessDialog = true
              }
            }
          }
        },
        enabled = !isSubmitting,
        modifier = Modifier.fillMaxWidth().height(60.dp).testTag("book_now_button"),
        shape = RoundedCornerShape(18.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = EmeraldPrimary,
            contentColor = PureWhite,
          ),
        contentPadding = PaddingValues(vertical = 14.dp),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = PureWhite,
            strokeWidth = 2.5.dp,
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = strings.submittingBooking,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        } else {
          Icon(
            imageVector = Icons.Filled.LocalShipping,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = strings.bookNowButton,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  if (showDatePicker) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
            showDatePicker = false
          }
        ) {
          Text("OK")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text(strings.cancelAction) }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showTimePicker) {
    val timePickerState =
      rememberTimePickerState(
        initialHour = selectedHour,
        initialMinute = selectedMinute,
        is24Hour = false,
      )
    AlertDialog(
      onDismissRequest = { showTimePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            selectedHour = timePickerState.hour
            selectedMinute = timePickerState.minute
            showTimePicker = false
          }
        ) {
          Text("OK")
        }
      },
      dismissButton = {
        TextButton(onClick = { showTimePicker = false }) { Text(strings.cancelAction) }
      },
      title = { Text(strings.selectTime) },
      text = { TimePicker(state = timePickerState) },
    )
  }

  if (showSuccessDialog) {
    AlertDialog(
      onDismissRequest = { showSuccessDialog = false },
      icon = {
        Icon(
          imageVector = Icons.Filled.CheckCircle,
          contentDescription = null,
          tint = EmeraldPrimary,
          modifier = Modifier.size(44.dp),
        )
      },
      title = {
        Text(
          text = strings.bookingSuccessTitle,
          fontWeight = FontWeight.Bold,
          color = NavyDeep,
        )
      },
      text = { Text(text = strings.bookingSuccessMessage) },
      confirmButton = {
        Button(
          onClick = {
            showSuccessDialog = false
            onNavigateToMyBookings()
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
        ) {
          Text(strings.viewMyBookings)
        }
      },
      dismissButton = {
        TextButton(onClick = { showSuccessDialog = false }) { Text(strings.bookAnother) }
      },
    )
  }
}

@Composable
private fun SectionCard(
  title: String,
  content: @Composable () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = NavyDeep,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(12.dp))
      content()
    }
  }
}
