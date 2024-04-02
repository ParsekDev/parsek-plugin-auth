package co.statu.rule.auth.error

import co.statu.parsek.model.Error

class InvalidSignature(
    statusMessage: String = "",
    extras: Map<String, Any> = mapOf()
) : Error(422, statusMessage, extras)