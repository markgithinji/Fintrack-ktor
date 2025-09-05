package com.fintrack.feature.transactions

object TransactionRepository {
    private val transactions = mutableListOf<Transaction>()
    private var nextId = 1

    fun getAll(): List<Transaction> = transactions

    fun getById(id: Int): Transaction? =
        transactions.find { it.id == id }

    fun add(transaction: Transaction): Transaction {
        val newTransaction = transaction.copy(id = nextId++)
        transactions.add(newTransaction)
        return newTransaction
    }

    fun update(id: Int, updated: Transaction): Transaction? {
        val index = transactions.indexOfFirst { it.id == id }
        if (index == -1) return null
        val newTransaction = updated.copy(id = id)
        transactions[index] = newTransaction
        return newTransaction
    }

    fun delete(id: Int): Boolean =
        transactions.removeIf { it.id == id }
}
