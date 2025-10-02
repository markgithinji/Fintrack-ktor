package feature.accounts.data

import com.fintrack.feature.accounts.data.model.AccountDto
import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.accounts.domain.Account


fun Account.toDto(
    income: Double? = null,
    expense: Double? = null,
    balance: Double? = null
): AccountDto = AccountDto(
    id = this.id,
    name = this.name,
    income = income,
    expense = expense,
    balance = balance
)

fun AccountDto.toDomain(userId: Int): Account = Account(
    id = this.id ?: 0,   // use 0 or ignore; DB will generate if inserting
    userId = userId,
    name = this.name
)

fun CreateAccountRequest.toDomain(userId: Int) = Account(
    id = 0, // Use 0 as temporary ID, repository will generate real ID
    userId = userId,
    name = name.trim()
)

fun UpdateAccountRequest.toDomain(userId: Int, accountId: Int) = Account(
    id = accountId,
    userId = userId,
    name = name.trim()
)
