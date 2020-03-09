package org.mechdancer.symbol.system

data class Stamped<T>(val stamp: Long, val data: T)
    : Comparable<Stamped<*>> {
    override fun compareTo(other: Stamped<*>) =
        stamp.compareTo(other.stamp)
}
