package co.statu.rule.auth.token

import co.statu.rule.token.type.TokenType
import java.util.*

class MagicRegisterToken : TokenType {
    override fun getExpireDateFromNow(): Long {
        val calendar = Calendar.getInstance()

        calendar.add(Calendar.MINUTE, 15)

        return calendar.timeInMillis
    }
}