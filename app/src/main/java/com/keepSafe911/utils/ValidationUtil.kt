class ValidationUtil {

    companion object {
        fun isValidEmail(toString: String): Boolean {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(toString).matches()
        }

        fun isRequiredField(toString: String): Boolean {
            return toString.isNotEmpty() && toString.isNotBlank()
        }

        fun isPasswordMatch(password: String, confirm_pass: String): Boolean {
            return password == confirm_pass
        }

        fun isValidPasswordLength(toString: String): Boolean {
            return toString.length >= 8
        }

        fun isPasswordValidate(password: String): Boolean {

            password.let {
                val passwordPattern = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[\\\\\\/%§\"&“|`´}{°><:.;#')(@_\$\"!?*=^-]).{8,}\$"
                val passwordMatcher = Regex(passwordPattern)

                return passwordMatcher.find(password) != null
            } ?: return false

        }


    }

    fun String.isValidMobile(): Boolean {
        return this.length == 10
    }


}


