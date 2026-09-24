package it.socialblock.platform.time

import javax.inject.Inject
import javax.inject.Singleton

interface TimeProvider {
    fun nowMillis(): Long
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
