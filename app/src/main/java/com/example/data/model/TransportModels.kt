package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class VehicleType(
  val label: String,
  val labelHi: String,
  val capacityHint: String,
  val capacityHintHi: String,
) {
  TATA_ACE("Tata Ace", "टाटा ऐस", "Up to 750 Kg", "750 किग्रा तक"),
  PICKUP("Pickup", "पिकअप", "1.5 – 2 Tons", "1.5 – 2 टन"),
  TATA_407("Tata 407", "टाटा 407", "2.5 – 3 Tons", "2.5 – 3 टन"),
  WHEELER_6("6 Wheeler", "6 व्हीलर (चक्का)", "7 – 9 Tons", "7 – 9 टन"),
  WHEELER_10("10 Wheeler", "10 व्हीलर (चक्का)", "15 – 16 Tons", "15 – 16 टन"),
  WHEELER_12("12 Wheeler", "12 व्हीलर (चक्का)", "20 – 21 Tons", "20 – 21 टन"),
  WHEELER_14("14 Wheeler", "14 व्हीलर (चक्का)", "25 Tons", "25 टन"),
  WHEELER_16("16 Wheeler", "16 व्हीलर (चक्का)", "28 – 30 Tons", "28 – 30 टन"),
  WHEELER_18("18 Wheeler", "18 व्हीलर (चक्का)", "32 – 35 Tons", "32 – 35 टन"),
  WHEELER_22("22 Wheeler", "22 व्हीलर (चक्का)", "40+ Tons", "40+ टन"),
  CONTAINER("Container", "कंटेनर", "7 – 18 Tons", "7 – 18 टन"),
  TRAILER("Trailer", "ट्रेलर", "20 – 45 Tons", "20 – 45 टन"),
  DUMPER("Dumper", "डंपर", "16 – 28 Tons", "16 – 28 टन"),
  ALL_TYPES(
    "All types of goods vehicles",
    "सभी प्रकार के माल वाहन",
    "All India Fleet",
    "ऑल इंडिया बेड़ा",
  );

  companion object {
    fun fromLabel(label: String): VehicleType =
      entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: ALL_TYPES
  }
}

enum class BookingStatus(
  val label: String,
  val labelHi: String,
) {
  NEW_BOOKING("New Booking", "नई बुकिंग"),
  PENDING("Pending", "लंबित (पेंडिंग)"),
  CONFIRMED("Confirmed", "कन्फर्म (स्वीकृत)"),
  VEHICLE_ASSIGNED("Vehicle Assigned", "वाहन आवंटित"),
  VEHICLE_DISPATCHED("Vehicle Dispatched", "वाहन रवाना"),
  DELIVERED("Delivered", "डिलीवरी पूर्ण"),
  CANCELLED("Cancelled", "रद्द");

  companion object {
    fun fromLabel(label: String): BookingStatus =
      entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: NEW_BOOKING
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

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val mobileNumber: String = "",
  val email: String = "",
  val city: String = "",
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
  val vehicleType: String = VehicleType.WHEELER_10.label,
  val driverName: String = "",
  val driverPhone: String = "",
  val availability: String = VehicleAvailability.AVAILABLE.label,
  val currentCity: String = "",
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
  val specialInstructions: String = "",
  val status: String = BookingStatus.NEW_BOOKING.label,
  val assignedVehicleId: String = "",
  val assignedVehicleNumber: String = "",
  val assignedDriverName: String = "",
  val assignedDriverPhone: String = "",
  val adminNotes: String = "",
  val estimatedFare: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
