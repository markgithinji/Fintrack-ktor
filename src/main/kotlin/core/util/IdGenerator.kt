package core.util

import com.github.f4b6a3.uuid.UuidCreator
import java.util.UUID

object IdGenerator {
    // Generates a UUID v7
    fun nextId(): UUID = UuidCreator.getTimeOrderedEpoch()
}
