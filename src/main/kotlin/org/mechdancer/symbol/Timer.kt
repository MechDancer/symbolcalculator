package org.mechdancer.symbol

class Timer(private val title: String) {
    private val list = mutableListOf(System.currentTimeMillis())

    fun mark() {
        list += System.currentTimeMillis()
    }

    fun display() {
        list += System.currentTimeMillis()
        list.zipWithNext { a, b -> b - a }
            .also { println("$title: $it, total = ${list.last() - list.first()}") }
    }
}
