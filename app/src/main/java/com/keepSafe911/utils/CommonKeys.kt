package com.keepSafe911.utils

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.math.BigDecimal

const val OUTPUT_DATE_FORMAT = "dd MMM yyyy"
const val OUTPUT_DATE_FORMAT2 = "MMM dd yyyy"
val PARSE_DATE_FORMAT = "dd/MM/yyyy"
const val INPUT_DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss"
const val OUTED_DATE = "MM-dd-yyyy"
const val OUTED_DATE2 = "dd-MMM-yyyy"
const val DELIVER_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS"
val MAP_ZOOM_VALUEW = 15F
val TYPE_AUTOCOMPLETE = "/autocomplete"
val OUT_JSON = "/json"
val PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place"
val KEY_GooglePlacesAPI = "AIzaSyBXyBLqyMjr59IhzckmlGiRxTJsw9IVunw"
val TIME_FORMAT = "hh:mm a"
val SHOW_DATE_TIME = "MM-dd-yyyy hh:mm a"
val INDIAN_DATE_TIME = "dd-MMM-yyyy hh:mm a"
val CHECK_DATE_TIME = "MM-dd-yyyy hh:mm"
val CHECK_DATE_TIME2 = "MM-dd-yyyy HH:mm"
val CHECK_DATE_TIME3 = "dd-MMM-yyyy HH:mm"
val CHECK_DATE_TIME4 = "dd-MMM-yyyy"
val TIME_FORMAT_24 = "HH:mm"
val TIME_FORMAT_24_WITH_SECONDS = "HH:mm:ss"
const val INPUT_CHECK_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
const val UNITED__CODE_POSITION: Int = 234
const val CACHE_FOLDER_NAME: String = "/KeepSafe911/"
const val USER_IMAGE_NAME: String = "ProfileImage.jpg"

const val LOGIN_RECORD_STATUS = 1
const val PIN_RECORD_STATUS = 2
const val LOGOUT_RECORD_STATUS = 3

const val DEVICE_TYPE_ID = 1
const val DEVICE_TYPE = "Android"

const val CHANGE_PAYMENT = 0
const val FREE_TRIAL_CODE = 1
const val MONTH_TRIAL_CODE = 2
const val YEAR_TRIAL_CODE = 3
const val MEMBER_MONTH_CODE = 4
const val MEMBER_YEAR_CODE = 5


const val meterToMiles = 1609.34
const val distanceLocationMeter = 5
const val multiplierForZoomLevel = 1900

/**
 * For payment amount
 */
/*
val ADMIN_MONTH_PAYMENT=0.1
val ADMIN_YEAR_PAYMENT=0.1
val USER_MONTH_PAYMENT=0.1
val USER_YEAR_PAYMENT=0.1
*/
const val FREE_PAYMENT = 0.1
const val ADMIN_MONTH_PAYMENT = 4.99
const val ADMIN_YEAR_PAYMENT = 49.99
const val USER_MONTH_PAYMENT = 0.99
const val USER_YEAR_PAYMENT = 9.99


const val FIND_YOUR_SELF_KEY = 1
const val REPORT_KEY = 2
const val SETTING_KEY = 4
const val PWNED: Int = 5
val SUPPORT_KEY=5

val CARD_NUMBER_TOTAL_SYMBOLS = 19 // size of pattern 0000 0000 0000 0000
val CARD_NUMBER_TOTAL_DIGITS = 16 // max numbers of digits in pattern: 0000 x 4
const val CARD_NUMBER_DIVIDER_MODULO = 5 // means divider position is every 5th symbol beginning with 1
val CARD_NUMBER_DIVIDER_POSITION =
    CARD_NUMBER_DIVIDER_MODULO - 1 // means divider position is every 4th symbol beginning with 0
val CARD_NUMBER_DIVIDER = ' ' // max numbers of digits in pattern: MM + YY
val CARD_DATE_DIVIDER_MODULO = 3 // means divider position is every 3rd symbol beginning with 1
const val IS_FOR_PAYMENT_KEY = "isForPayment"
const val IS_FROM_LOGIN_KEY = "isFromLogin"
val FILE_SELECT_CODE=1
val START_ACTIVITY_REQUESTCODE=2

var inside: Boolean = true
val MEDIA_TYPE_TEXT = "text/plain".toMediaTypeOrNull()
const val nullData = "null"

const val LIKE = 1
const val SHARE = 3
const val COMMENT = 2

val videoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
//get different card number
//https://www.freeformatter.com/credit-card-number-generator-validator.html


/*[
KeyID: 4tbW7uf5yrMr234hwDzT9D2x9JvjV4K37VCsRCH2HXH9UL4kcbPCVz6dz22m79zG
PublicKey: BPGMwattA2Z7yJXETMRgboJ5/1SHNghJlwfWAXTdsXPm8IBlHQKWzWBRM9CxMEOoXNtI4kOtbr4bRHXMqgycABI=
PublicKeyHash: Rf0TSQel1e4+cCxP9y5dPzYkJ8App1dvbfzhMPqKqfk=
CreateTimeUTC: 2019-03-12T06:27:33.18
]*/


/**
 * agora detail
 * Obtain a temp Access Token at https://dashboard.agora.io
 */
const val agora_access_token = "00622ad6862b6ab4644838bac242ba73e5eIABDsj5YuVWL4YVgPNTZb5mbZWOMO0DktThS/VRWWzkMcjLRTXgAAAAAEAAr6nfpdCPDYQEAAQB0I8Nh"
const val liveStreamMinimumDurationKey = "liveStreamMinimumDuration"
const val liveStreamMinimumDuration = 900000L

/**
 * bank benefit link
 */
const val bankLink = "https://www.recoup.ai/oldme/"
const val benefitLink = "https://oldme.benefithub.com/Account/Login?ReturnUrl=%2f#"