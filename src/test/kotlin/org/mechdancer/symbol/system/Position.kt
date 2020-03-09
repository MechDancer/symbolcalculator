package org.mechdancer.symbol.system

import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector

/**
 * 每个定位点对应标签的一次有意义的移动
 */
data class Position(
    val beacon: Beacon,
    val time: Long
) : Comparable<Position> {
    fun toVector(): ExpressionVector {
        val postfix = if (time > 0) "${beacon.id}_$time" else beacon.id.toString()
        return ExpressionVector(mapOf(
            x to Variable("x$postfix"),
            y to Variable("y$postfix"),
            z to Variable("z$postfix")))
    }

    override fun compareTo(other: Position) =
        beacon.compareTo(other.beacon)
            .takeIf { it != 0 }
        ?: time.compareTo(other.time)

    private companion object {
        val x = Variable("x")
        val y = Variable("y")
        val z = Variable("z")
    }
}
