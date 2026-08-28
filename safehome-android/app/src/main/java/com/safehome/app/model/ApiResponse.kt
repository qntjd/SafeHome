package com.safehome.app.model

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val email: String,
    val nickname: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val nickname: String,
    val homeLat: Double?,
    val homeLng: Double?
)

data class StartTripRequest(
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val estimatedMinutes: Int
)

data class TripResponse(
    val id: String,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val departureAt: String,
    val expectedArrivalAt: String,
    val arrivedAt: String?,
    val status: String,
    val shareToken: String?
)

data class FacilityResponse(
    val id: String,
    val type: String,
    val lat: Double,
    val lng: Double,
    val name: String?,
    val districtName: String?
)

data class FacilityCountResponse(
    val cctvCount: Int,
    val bellCount: Int,
    val policeCount: Int
)

data class AlertResponse(
    val id: String,
    val title: String,
    val message: String,
    val districtName: String,
    val level: String,
    val issuedAt: String
)
data class SubscribeRequest(
    val alertType: String = "ALL",
    val sidoName : String,
    val sigunguName: String?,
    val label : String?,
    val isMyLocation: Boolean = false,
    val category: String = "OTHER"

)
data class SubscriptionResponse(
    val id: String,
    val sidoName: String,
    val sigunguName: String?,
    val label: String?,
    val isMyLocation: Boolean,
    val isActive: Boolean,
    val category : String
)

data class NewsResponse(
    val id: String,
    val title: String,
    val description: String?,
    val url: String,
    val source: String?,
    val keyword: String?,
    val publishedAt: String
)

data class NewsPageResponse(
    val articles: List<NewsResponse>,
    val currentPages: Int,
    val totalPages: Int,
    val totalElements: Long,

)

data class DistrictCrimeResponse(
    val districtCode: String,
    val districtName: String,
    val crimeByType: Map<String, Int>,
    val totalCount: Int
)

data class AllDistrictCrimeResponse(
    val districts: List<DistrictCrimeResponse>
)

data class SosRecipientRequest(
    val contactName: String?,
    val phoneNumber: String,
    val status: String,
    val errorMessage: String?= null
)

data class SosCreateLogRequest(
    val triggerType: String,
    val lat: Double?,
    val lng: Double?,
    val address: String?,
    val policeReported: Boolean,
    val recipients: List<SosRecipientRequest>
)

data class SosRecipientResponse(
    val contactName: String?,
    val phoneNumber: String,
    val status: String,
    val errorMessage: String?
)

data class SosLogResponse(
    val id: String,
    val triggerType: String,
    val lat: Double?,
    val lng: Double?,
    val address: String?,
    val policeReported: Boolean,
    val createdAt: String,
    val recipients: List<SosRecipientResponse>
)