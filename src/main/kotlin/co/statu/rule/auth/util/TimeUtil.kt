package co.statu.rule.auth.util

object TimeUtil {
    fun getDifferenceInSeconds(date: Long): Long {
        // Get the current time in milliseconds
        val currentTime = System.currentTimeMillis()

        // Calculate the difference in milliseconds
        val differenceInMillis = currentTime - date

        // Convert the difference to seconds
        val differenceInSeconds = differenceInMillis / 1000

        return differenceInSeconds
    }

    fun getTimeAfterSeconds(currentTime: Long, secondsToAdd: Long): Long {
        // Convert the seconds to milliseconds and add to the current time
        return currentTime + (secondsToAdd * 1000)
    }
}