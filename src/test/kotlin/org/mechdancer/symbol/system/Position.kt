package org.mechdancer.symbol.system

import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector

/**
 * 每个定位点对应标签的一次有意义的移动
 */
data class Position(
    val beacon: Beacon,
    val time: Long
) : Comparable<Position> {
    val variables by lazy {
        val postfix = if (time > 0) "${beacon.id}_$time" else beacon.id.toString()
        listOf(Variable("x$postfix"),
               Variable("y$postfix"),
               Variable("z$postfix"))
    }

    fun isStatic() = time < 0

    fun toVector() =
        ExpressionVector(prefix.zip(variables).toMap())

    fun toVector(value: Vector3D) =
        ExpressionVector(variables.zip(value.toList().map(::Constant)).toMap())

    override fun compareTo(other: Position) =
        beacon.compareTo(other.beacon)
            .takeIf { it != 0 }
        ?: time.compareTo(other.time)

    private companion object {
        val x = Variable("x")
        val y = Variable("y")
        val z = Variable("z")
        val prefix = listOf(x, y, z)
    }
}
