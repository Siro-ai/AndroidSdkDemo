package com.siro.demo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <T> Flow<T>.collectRunningLast(n: Int, fill: T? = null): Flow<List<T>> = flow {
    val buffer = ArrayDeque<T>().apply {
        addAll((0 until n).mapNotNull { fill })
    }
    collect { value ->
        buffer.add(value)
        if (buffer.size > n) {
            buffer.removeFirst()
        }
        emit(buffer.toList())
    }
}
