package co.statu.rule.auth

import co.statu.parsek.api.config.PluginConfigManager
import io.vertx.core.json.JsonObject
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Recaptcha V3
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class GoogleRecaptcha(private val authPlugin: AuthPlugin) {
    private val pluginConfigManager by lazy {
        authPlugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<AuthConfig>
    }

    companion object {
        const val RECAPTCHA_SERVICE_URL = "https://www.google.com/recaptcha/api/siteverify"
    }

    /**
     * checks if a user is valid
     * @param clientRecaptchaResponse
     * @return true if human, false if bot
     */
    fun isValid(clientRecaptchaResponse: String?): Boolean {
        return try {
            val recaptchaConfig = pluginConfigManager.config.recaptchaConfig
            val isRecaptchaEnabled = recaptchaConfig.enabled
            val recaptchaSecret = recaptchaConfig.secret

            if (!isRecaptchaEnabled) {
                return true
            }

            if (clientRecaptchaResponse == null || "" == clientRecaptchaResponse) {
                return false
            }

            val obj = URL(RECAPTCHA_SERVICE_URL)
            val con = obj.openConnection() as HttpsURLConnection
            con.requestMethod = "POST"
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5")

            //add client result as post parameter
            val postParams = "secret=$recaptchaSecret&response=$clientRecaptchaResponse"

            // send post request to google recaptcha server
            con.doOutput = true
            val wr = DataOutputStream(con.outputStream)
            wr.writeBytes(postParams)
            wr.flush()
            wr.close()
            val responseCode = con.responseCode
//            println("Post parameters: $postParams")
//            println("Response Code: $responseCode")
            val `in` = BufferedReader(
                InputStreamReader(
                    con.inputStream
                )
            )
            var inputLine: String?
            val response = StringBuffer()
            while (`in`.readLine().also { inputLine = it } != null) {
                response.append(inputLine)
            }
            `in`.close()
//            println(response.toString())

            //Parse JSON-response
            val json = JsonObject(response.toString())
            val success = json.getBoolean("success")
            val score = json.getDouble("score")
//            println("success : $success")
//            println("score : $score")

            //result should be successful and spam score above 0.5
            success && score >= 0.5
        } catch (exception: Exception) {
            false
        }
    }
}