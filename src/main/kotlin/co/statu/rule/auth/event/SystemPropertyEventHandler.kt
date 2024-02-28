package co.statu.rule.auth.event

import co.statu.rule.auth.AuthPlugin
import co.statu.rule.systemProperty.db.dao.SystemPropertyDao
import co.statu.rule.systemProperty.event.SystemPropertyListener

class SystemPropertyEventHandler : SystemPropertyListener {
    override suspend fun onReady(systemPropertyDao: SystemPropertyDao) {
        AuthPlugin.externalTables.add(systemPropertyDao)
    }
}