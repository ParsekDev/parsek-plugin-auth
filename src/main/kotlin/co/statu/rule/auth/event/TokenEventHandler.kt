package co.statu.rule.auth.event

import co.statu.parsek.api.annotation.EventListener
import co.statu.rule.auth.token.*
import co.statu.rule.token.event.TokenEventListener
import co.statu.rule.token.type.TokenType

@EventListener
class TokenEventHandler : TokenEventListener {

    override fun registerTokenType(tokenTypeList: MutableList<TokenType>) {
        tokenTypeList.addAll(
            listOf(
                AuthenticationToken(),
                ConfirmDeleteAccountToken(),
                MagicLoginToken(),
                MagicRegisterToken(),
                RegisterToken()
            )
        )
    }
}