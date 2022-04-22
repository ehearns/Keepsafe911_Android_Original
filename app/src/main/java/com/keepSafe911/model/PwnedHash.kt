package com.keepSafe911.model

/**
 * A hash that may indicate a password was pwned.
 * @author gideon
 */
class PwnedHash(val hash: String, val count: Int) {

    override fun toString(): String {
        return "$hash:$count"
    }
}
