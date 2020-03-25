package org.mechdancer.symbol.system

import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.symbol.`^`
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.core.VariableSpace
import org.mechdancer.symbol.minus
import org.mechdancer.symbol.sqrt
import org.mechdancer.symbol.sum

/**
 * 每个定位点对应标签的一次有意义的移动
 */
data class Position(
    val beacon: Beacon,
    val time: Long
) : Comparable<Position> {
    val space by lazy {
        val postfix = if (time > 0) "${beacon.id}_$time" else beacon.id.toString()
        VariableSpace(listOf(Variable("x$postfix"),
                             Variable("y$postfix"),
                             Variable("z$postfix")))
    }

    fun isStatic() = time <= 0

    infix fun euclid(others: Position) =
        sqrt(space.variables.zip(others.space.variables) { a, b -> (a - b) `^` 2 }.sum())

    infix fun euclid(others: Vector3D) =
        sqrt(space.variables.zip(others.toList()) { a, b -> (a - b) `^` 2 }.sum())

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
