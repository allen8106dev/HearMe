package com.allen.hearme.core.common.time

interface TimeProvider {
    fun currentTimeMillis(): Long
    fun currentEpochSeconds(): Long
}

class DefaultTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1000L
}
