package com.fintrack.core

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

val appSerializersModule = SerializersModule {
    contextual(UUIDSerializer)
}