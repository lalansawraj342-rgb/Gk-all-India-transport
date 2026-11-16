package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

val SUPPORT_PHONE_NUMBERS = listOf("9142631727", "7759030605", "9153935210")

enum class VehicleType(
  val label: String,
  val labelHi: String,
  val capacityHint: String,
  val capacityHintHi: String,
) {
  TATA_ACE("Tata Ace", "टाटा ऐस", "Up to 750 Kg", "750 किग्रा तक"),
  PICKUP_TRUCK("Pickup Truck", "पिकअप ट्रक", "1.5 – 2 Tons", "1.5 – 2 टन"),
  BOLERO_PICKUP("Bolero Pickup", "बोलेरो पिकअप", "1.5 – 1.7 Tons", "1.5 – 1.7 टन"),
  TRUCK_407("407 Truck", "407 ट्रक", "2.5 – 3.5 Tons", "2.5 – 3.5 टन"),
  TRUCK_709("709 Truck", "709 ट्रक", "4.5 – 5.5 Tons", "4.5 – 5.5 टन"),
  TRUCK_1109("1109 Truck", "1109 ट्रक", "7.5 – 9 Tons", "7.5 – 9 टन"),
  WHEELER_14("14 Wheeler Truck", "14 व्हीलर ट्रक", "25 Tons", "25 टन"),
  WHEELER_16("16 Wheeler Truck", "16 व्हीलर ट्रक", "28 – 30 Tons", "28 – 30 टन"),
  WHEELER_18("18 Wheeler Truck", "18 व्हीलर ट्रक", "32 – 35 Tons", "32 – 35 टन"),
  CONTAINER_TRUCK("Container Truck", "कंटेनर ट्रक", "7 – 20 Tons", "7 – 20 टन"),
  TRAILER_TRUCK("Trailer Truck", "ट्रेलर ट्रक", "20 – 45 Tons", "20 – 45 टन"),
  DUMPER("Dumper", "डंपर", "16 – 28 Tons", "16 – 28 टन"),
  OTHER_COMMERCIAL(
    "Other Commercial Vehicles",
    "अन्य कमर्शियल वाहन",
    "All India Fleet",
    "ऑल इंडिया बेड़ा",
  );

  companion object {
    fun fromLabel(label: String): VehicleType {
      entries.firstOrNull { it.label.equals(label, ignoreCase = true) }?.let { return it }
      return when (label.trim().lowercase()) {
        "pickup" -> PICKUP_TRUCK
        "tata 407" -> TRUCK_407
        "6 wheeler", "10 wheeler", "12 wheeler" -> TRUCK_1109
        "14 wheeler" -> WHEELER_14
        "16 wheeler" -> WHEELER_16
        "18 wheeler", "22 wheeler" -> WHEELER_18
        "container" -> CONTAINER_TRUCK
        "trailer" -> TRAILER_TRUCK
        else -> OTHER_COMMERCIAL
      }
    }
  }
}

enum class BookingStatus(
  val label: String,
  val labelHi: String,
) {
  PENDING("Pending", "लंबित (पेंडिंग)"),
  ACCEPTED("Accepted", "स्वीकृत (Accepted)"),
  VEHICLE_ASSIGNED("Vehicle Assigned", "वाहन आवंटित"),
  DRIVER_ASSIGNED("Driver Assigned", "ड्राइवर आवंटित"),
  ON_THE_WAY("On The Way", "रास्ते में (On The Way)"),
  GOODS_PICKED_UP("Goods Picked Up", "माल लोड हो गया"),
  IN_TRANSIT("In Transit", "ट्रांजिट में"),
  DELIVERED("Delivered", "डिलीवरी पूर्ण"),
  CANCELLED("Cancelled", "रद्द");

  fun isActiveTrip(): Boolean =
    this != DELIVERED && this != CANCELLED

  fun isPreviousTrip(): Boolean =
    this == DELIVERED || this == CANCELLED

  fun canCustomerCancel(): Boolean =
    this == PENDING || this == ACCEPTED

  companion object {
    val trackableSteps: List<BookingStatus> = listOf(
      PENDING,
      ACCEPTED,
      VEHICLE_ASSIGNED,
      DRIVER_ASSIGNED,
      ON_THE_WAY,
      GOODS_PICKED_UP,
      IN_TRANSIT,
      DELIVERED,
    )

    fun fromLabel(label: String): BookingStatus {
      entries.firstOrNull { it.label.equals(label, ignoreCase = true) }?.let { return it }
      return when (label.trim().lowercase()) {
        "new booking" -> PENDING
        "confirmed" -> ACCEPTED
        "vehicle dispatched" -> ON_THE_WAY
        else -> PENDING
      }
    }
  }
}

enum class VehicleAvailability(
  val label: String,
  val labelHi: String,
) {
  AVAILABLE("Available", "उपलब्ध"),
  ON_TRIP("On Trip", "यात्रा पर"),
  MAINTENANCE("Maintenance", "मरम्मत में");

  companion object {
    fun fromLabel(label: String): VehicleAvailability =
      entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: AVAILABLE
  }
}

enum class DriverAvailability(
  val label: String,
  val labelHi: String,
) {
  AVAILABLE("Available", "उपलब्ध"),
  ON_TRIP("On Trip", "यात्रा पर"),
  OFF_DUTY("Off Duty", "अवकाश पर");

  companion object {
    fun fromLabel(label: String): DriverAvailability =
      entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: AVAILABLE
  }
}

enum class DriverTripStatus(
  val label: String,
  val labelHi: String,
) {
  IDLE("Idle", "खाली"),
  EN_ROUTE_TO_PICKUP("En Route to Pickup", "पिकअप के लिए रवाना"),
  LOADING("Loading", "लोडिंग हो रही है"),
  IN_TRANSIT("In Transit", "ट्रांजिट में"),
  COMPLETED("Completed", "यात्रा पूर्ण");

  companion object {
    fun fromLabel(label: String): DriverTripStatus =
      entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: IDLE
  }
}

enum class PaymentStatus(
  val label: String,
  val labelHi: String,
) {
  UNPAID("Unpaid", "अवैतनिक (Unpaid)"),
  ADVANCE_PAID("Advance Paid", "अग्रिम भुगतान"),
  PAID("Paid", "भुगतान पूर्ण (Paid)");

  companion object {
    fun fromLabel(label: String): PaymentStatus =
      entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: UNPAID
  }
}

enum class PaymentMethod(
  val label: String,
  val labelHi: String,
) {
  NONE("None", "कोई नहीं"),
  CASH("Cash", "नकद (Cash)"),
  UPI("UPI", "UPI पेमेंट"),
  ONLINE("Online", "ऑनलाइन पेमेंट"),
  ADVANCE("Advance", "अग्रिम (Advance)");

  companion object {
    val selectableMethods: List<PaymentMethod> = listOf(CASH, UPI, ONLINE, ADVANCE)

    fun fromLabel(label: String): PaymentMethod =
      entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: NONE
  }
}

enum class PaymentRecordStatus(
  val label: String,
  val labelHi: String,
) {
  PENDING_VERIFICATION("Pending Verification", "सत्यापन लंबित"),
  PAID("Paid", "भुगतान सफल"),
  REJECTED("Rejected", "अस्वीकृत");

  companion object {
    fun fromLabel(label: String): PaymentRecordStatus =
      entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: PENDING_VERIFICATION
  }
}

enum class NotificationType(val code: String) {
  BOOKING_REQUEST("BOOKING_REQUEST"),
  BOOKING_ACCEPTED("BOOKING_ACCEPTED"),
  VEHICLE_ASSIGNED("VEHICLE_ASSIGNED"),
  DELIVERY_COMPLETED("DELIVERY_COMPLETED"),
  ADMIN_MESSAGE("ADMIN_MESSAGE");

  companion object {
    fun fromCode(code: String): NotificationType =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ADMIN_MESSAGE
  }
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val mobileNumber: String = "",
  val email: String = "",
  val city: String = "",
  val address: String = "",
  val alternatePhone: String = "",
  val preferredLanguage: String = "en",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AdminUser(
  @DocumentId val id: String = "",
  val uid: String = "",
  val email: String = "",
  val name: String = "",
  val addedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FleetVehicle(
  @DocumentId val id: String = "",
  val vehicleNumber: String = "",
  val vehicleType: String = VehicleType.WHEELER_14.label,
  val driverName: String = "",
  val driverPhone: String = "",
  val availability: String = VehicleAvailability.AVAILABLE.label,
  val currentCity: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Driver(
  @DocumentId val id: String = "",
  val name: String = "",
  val mobileNumber: String = "",
  val licenseNumber: String = "",
  val assignedVehicleId: String = "",
  val assignedVehicleNumber: String = "",
  val availability: String = DriverAvailability.AVAILABLE.label,
  val tripStatus: String = DriverTripStatus.IDLE.label,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Booking(
  @DocumentId val id: String = "",
  val userId: String = "",
  val bookingCode: String = "",
  val customerName: String = "",
  val customerPhone: String = "",
  val pickupLocation: String = "",
  val deliveryLocation: String = "",
  val bookingDateTimeMillis: Long = 0L,
  val bookingDateLabel: String = "",
  val bookingTimeLabel: String = "",
  val vehicleType: String = VehicleType.TATA_ACE.label,
  val goodsDescription: String = "",
  val goodsWeight: String = "",
  val goodsPhotos: List<String> = emptyList(),
  val specialInstructions: String = "",
  val status: String = BookingStatus.PENDING.label,
  val assignedVehicleId: String = "",
  val assignedVehicleNumber: String = "",
  val assignedDriverId: String = "",
  val assignedDriverName: String = "",
  val assignedDriverPhone: String = "",
  val adminNotes: String = "",
  val estimatedFare: String = "",
  val totalFareAmount: Double = 0.0,
  val paidAmount: Double = 0.0,
  val paymentStatus: String = PaymentStatus.UNPAID.label,
  val paymentMethod: String = PaymentMethod.NONE.label,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PaymentRecord(
  @DocumentId val id: String = "",
  val bookingId: String = "",
  val bookingCode: String = "",
  val userId: String = "",
  val customerName: String = "",
  val amount: Double = 0.0,
  val method: String = PaymentMethod.CASH.label,
  val status: String = PaymentRecordStatus.PENDING_VERIFICATION.label,
  val referenceId: String = "",
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AppNotification(
  @DocumentId val id: String = "",
  val recipientUserId: String = "",
  val senderUserId: String = "",
  val bookingId: String = "",
  val bookingCode: String = "",
  val type: String = NotificationType.ADMIN_MESSAGE.code,
  val title: String = "",
  val message: String = "",
  val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
)
