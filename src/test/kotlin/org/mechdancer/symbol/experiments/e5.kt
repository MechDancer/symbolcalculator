package org.mechdancer.symbol.experiments

import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.*
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.VariableSpace
import kotlin.math.abs
import kotlin.math.sqrt

// 地图

private val engine = java.util.Random()
private const val maxMeasure = 30.0
private val interval = maxMeasure / sqrt(2.0)

private val BEACONS = listOf(
    vector3D(0, 0, 0),
    vector3D(interval, 0, 0),
    vector3D(interval, interval, 0),
    vector3D(0, interval, 0))

private fun measure(d: Double) = d * (1 + 1e-3 * engine.nextGaussian()) + 5e-3 * engine.nextGaussian()

fun main() {
    val measures = BEACONS.map { a -> BEACONS.map { b -> measure(a euclid b) } }
    val space = BEACONS.mapIndexed { i, _ -> variables("x$i", "y$i", "z$i").variables }
                    .flatten()
                    .toSet()
                    .let(::VariableSpace) - variables("x0", "y0", "z0", "y1", "z1", "z2")
    val error = measures
        .mapIndexed { i, distances ->
            distances.mapIndexed { j, distance ->
                sqrt((Variable("x$i") - Variable("x$j") `^` 2) +
                     (Variable("y$i") - Variable("y$j") `^` 2) +
                     (Variable("z$i") - Variable("z$j") `^` 2)) - distance
            }
        }
        .flatten()
        .let { list -> list.sumBy { it `^` 2 } / list.size }
        .substitute {
            this[Variable("x0")] = 0
            this[Variable("y0")] = 0
            this[Variable("z0")] = 0

            this[Variable("y1")] = 0
            this[Variable("z1")] = 0

            this[Variable("z2")] = 0
        }

    val remote = remoteHub("定位优化").apply { openAllNetworks(); println(networksInfo()) }

    val f = dampingNewton(error, space)
    val init = space.ordinaryField.expressions.mapValues { Constant(1.0) }.let(::ExpressionVector)

    println(BEACONS)

    recurrence(init to .0) { (p, _) -> f(p) }
        .take(10000)
        .firstOrLast { (p, s) ->
            remote.paint(p)
            abs(s) < 5e-4
        }
        .first
        .also { println(it) }
}
