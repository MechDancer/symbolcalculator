package org.mechdancer.symbol.system

/**
 * 每个定位点对应标签的一次有意义的移动
 */
data class Position(
    val beacon: Beacon,
    val time: Long
) : Comparable<Position> {
    override fun compareTo(other: Position) =
        beacon.compareTo(other.beacon)
            .takeIf { it != 0 }
        ?: time.compareTo(other.time)
}
