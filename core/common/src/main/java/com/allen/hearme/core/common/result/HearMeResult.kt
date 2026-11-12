package com.allen.hearme.core.common.result

sealed interface HearMeResult<out T> {
    data class Success<out T>(val data: T) : HearMeResult<T>
    data class Error(val throwable: Throwable, val message: String? = throwable.message) : HearMeResult<Nothing>
    data object Loading : HearMeResult<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun exceptionOrNull(): Throwable? = when (this) {
        is Error -> throwable
        else -> null
    }
}
