package com.bytestream.config

import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import java.time.Clock


@Factory
class ClockFactory {
    @Singleton
    fun clockProvider(): Clock {
        return Clock.systemUTC()
    }
    
}