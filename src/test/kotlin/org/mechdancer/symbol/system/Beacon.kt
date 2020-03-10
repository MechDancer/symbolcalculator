package org.mechdancer.symbol.system

/**
 * 每个标签对应于一个特定的物理实体
 * 每次有意义的标签移动，对应于一个特定的定位点
 */
inline class Beacon(val id: Int) : Comparable<Beacon> {
    fun static() = Position(this, -1)
    fun move(t: Long) = Position(this, t)
    override fun compareTo(other: Beacon) = id.compareTo(other.id)
    override fun toString() = "[$id]"
}
