package core

import de.mkammerer.argon2.Argon2Factory

object PasswordHasher {
    private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

    // Argon2 parameters: 3 iterations, 64MB memory, 1 thread
    private const val ITERATIONS = 3
    private const val MEMORY = 65536
    private const val PARALLELISM = 1

    /**
     * Hashes a password using Argon2id.
     */
    fun hash(password: String): String {
        val chars = password.toCharArray()
        return try {
            argon2.hash(ITERATIONS, MEMORY, PARALLELISM, chars)
        } finally {
            argon2.wipeArray(chars)
        }
    }

    /**
     * Verifies a password against an Argon2id hash.
     */
    fun verify(password: String, hash: String): Boolean {
        val chars = password.toCharArray()
        return try {
            if (isArgon2Hash(hash)) {
                argon2.verify(hash, chars)
            } else {
                false
            }
        } finally {
            argon2.wipeArray(chars)
        }
    }

    private fun isArgon2Hash(hash: String): Boolean {
        return hash.startsWith("\$argon2id")
    }
}
