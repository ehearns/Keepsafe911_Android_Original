package com.keepSafe911.model

class PhoneCountryCode() {
    var countryName = ""
    var countryCode = ""
    var code = ""
    var isSelected = false
    var flag = -1

    constructor(countryName: String, countryCode: String, code: String, flag: Int, isSelected: Boolean) : this() {
        this.countryName = countryName
        this.countryCode = countryCode
        this.code = code
        this.flag = flag
        this.isSelected = isSelected
    }

    override fun toString(): String {
        return "$countryCode $countryName"
    }
}