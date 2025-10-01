package feature.accounts.data

import com.fintrack.feature.accounts.data.model.AccountDto
import feature.accounts.domain.Account


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
