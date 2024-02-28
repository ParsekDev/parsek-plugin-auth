package co.statu.rule.auth.event

import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.token.*
import co.statu.rule.token.event.TokenEventListener
import co.statu.rule.token.provider.TokenProvider
import co.statu.rule.token.type.TokenType

class TokenEventHandler : TokenEventListener {
    override suspend fun onReady(tokenProvider: TokenProvider) {
        AuthPlugin.tokenProvider = tokenProvider
    }

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