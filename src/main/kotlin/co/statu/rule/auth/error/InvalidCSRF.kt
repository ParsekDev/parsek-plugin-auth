package co.statu.rule.auth.error

import co.statu.parsek.model.Error

class InvalidCSRF(
    statusMessage: String = "",
    extras: Map<String, Any> = mapOf()
) : Error(401, statusMessage, extras)