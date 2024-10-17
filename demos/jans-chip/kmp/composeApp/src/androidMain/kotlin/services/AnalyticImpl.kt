package services

import com.example.fido2.services.Analytic
import com.example.fido2.services.Logger

class AnalyticImpl(private val logger: Logger): Analytic {
    override fun logEvent(event: String) {
        logger.log("Event \"$event\" sent to analytic by Android implementation")
    }
}