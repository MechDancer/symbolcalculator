package org.mechdancer.symbol.system

/**
 * 每次测量在两个定位点之间建立一个关系
 */
data class Measure(
    val a: Position,
    val b: Position,
    val time: Long,
    val distance: Double
) {
    init { // 这种关系对两个标签来说是无序的
        require(a.beacon.id < b.beacon.id)
    }
}
