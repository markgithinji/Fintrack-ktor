package core

import kotlinx.serialization.Serializable


data class Account(
    val id: Int,
    val userId: Int,
    val name: String
)

@Serializable
data class AccountDto(
    val id: Int? = null,
    val name: String,
    val balance: Double? = null,
)

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
