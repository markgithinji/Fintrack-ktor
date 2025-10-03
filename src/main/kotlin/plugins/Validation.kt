package plugins

import feature.accounts.domain.configureAccountValidation
import feature.auth.domain.configureAuthValidation
import feature.budget.domain.configureBudgetValidation
import feature.transaction.domain.configureTransactionValidation
import feature.user.domain.configureUserValidation
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