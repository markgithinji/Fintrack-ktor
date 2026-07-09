package plugins

import com.fintrack.feature.accounts.configureAccountValidation
import com.fintrack.feature.auth.configureAuthValidation
import com.fintrack.feature.budget.configureBudgetValidation
import com.fintrack.feature.transaction.configureTransactionValidation
import com.fintrack.feature.user.configureUserValidation
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*

fun Application.configureValidation() {
    install(RequestValidation) {
        configureAccountValidation()
        configureAuthValidation()
        configureBudgetValidation()
        configureTransactionValidation()
        configureUserValidation()
    }
}
