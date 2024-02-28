package co.statu.rule.auth.token

import co.statu.rule.token.type.TokenType
import java.util.*

class AuthenticationToken : TokenType {
    override fun getExpireDateFromNow(): Long {
        val calendar = Calendar.getInstance()

        calendar.add(Calendar.MONTH, 1)

        return calendar.timeInMillis
    }
}