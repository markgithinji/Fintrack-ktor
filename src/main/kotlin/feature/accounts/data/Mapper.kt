package feature.accounts.data

import feature.accounts.domain.Account


fun Account.toDto(balance: Double? = null): AccountDto = AccountDto(
    id = this.id,
    name = this.name,
    balance = balance
)

fun AccountDto.toDomain(userId: Int): Account = Account(
    id = this.id ?: 0,   // use 0 or ignore; DB will generate if inserting
    userId = userId,
    name = this.name
)
