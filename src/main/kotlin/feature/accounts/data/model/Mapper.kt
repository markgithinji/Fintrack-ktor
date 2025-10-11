package feature.accounts.data

import com.fintrack.feature.accounts.data.model.AccountDto
import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.accounts.domain.Account
import java.util.UUID

fun Account.toDto(income: Double, expense: Double, balance: Double): AccountDto = AccountDto(
    id = requireNotNull(id) { "Account must have an ID to convert to DTO" }.toString(),
    name = name,
    income = income,
    expense = expense,
    balance = balance
)

fun CreateAccountRequest.toDomain(userId: UUID): Account = Account(
    userId = userId,
    name = name
)

fun UpdateAccountRequest.toDomain(userId: UUID, accountId: UUID): Account = Account(
    id = accountId,
    userId = userId,
    name = name
)
