package com.example.fido2.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDetails(
    var sub: String,
    val name: String? = "",
    val nickname: String? = "",
    val given_name: String? = "",
    val middle_name: String? = "",
    val inum: String? = "",
    val family_name: String? = "",
    val jti: String,
    val client_id: String,
    val jansAdminUIRole: Array<String>? = arrayOf()
) {
    fun info(): HashMap<String, String> {
        var result = HashMap<String, String>()
        result.put("sub", sub)
        if (name?.isNotEmpty() == true) {
            result.put("name", name)
        }
        if (nickname?.isNotEmpty() == true) {
            result.put("nickname", nickname)
        }
        if (given_name?.isNotEmpty() == true) {
            result.put("given_name", given_name)
        }
        if (middle_name?.isNotEmpty() == true) {
            result.put("middle_name", middle_name)
        }
        if (inum?.isNotEmpty() == true) {
            result.put("inum", inum)
        }
        if (family_name?.isNotEmpty() == true) {
            result.put("family_name", family_name)
        }
        result.put("jti", jti)
        result.put("client_id", client_id)
        if (jansAdminUIRole?.isNotEmpty() == true) {
            result.put("jansAdminUIRole", jansAdminUIRole.map { it }.toString())
        }

        return result
    }
}