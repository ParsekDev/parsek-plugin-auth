package co.statu.rule.auth

enum class PanelPermission {
    ACCESS_PANEL;

    override fun toString(): String {
        return super.toString().lowercase()
    }
}