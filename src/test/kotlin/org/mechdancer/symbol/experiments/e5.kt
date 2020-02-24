package org.mechdancer.symbol.experiments

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.core.rowView
import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.remote.presets.RemoteHub
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.*
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.VariableSpace
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.sqrt

// 地图

private val engine = java.util.Random()
private const val maxMeasure = 30.0
private val interval = maxMeasure / sqrt(2.0)

private val BEACONS = listOf(
    vector3D(0, 0, 0),
    vector3D(interval, 0, 0),
    vector3D(interval, interval, 0),
    vector3D(0, interval, 0),
    vector3D(2 * interval, 0, 0),
    vector3D(2 * interval, interval, 0))

private val ZEROS = variables("x0", "y0", "z0", "y1", "z1", "z2")

private fun measure(d: Double) = d * (1 + 1e-3 * engine.nextGaussian()) + 5e-3 * engine.nextGaussian()

fun main() {
    val measures = BEACONS.map { a -> BEACONS.map { b -> measure(a euclid b) } }
    val space = BEACONS.mapIndexed { i, _ -> variables("x$i", "y$i", "z$i").variables }
                    .flatten()
                    .toSet()
                    .let(::VariableSpace) - ZEROS
    val error = measures
        .mapIndexed { i, distances ->
            distances.mapIndexed { j, distance ->
//                if (distance < maxMeasure)
                sqrt((Variable("x$i") - Variable("x$j") `^` 2) +
                     (Variable("y$i") - Variable("y$j") `^` 2) +
                     (Variable("z$i") - Variable("z$j") `^` 2)) - distance
//                else
//                    Constant.`0`
            }
        }
        .flatten()
        .let { list -> list.sumBy { it `^` 2 } / list.size }
        .substitute { for (v in ZEROS.variables) this[v] = 0 }

    val remote = remoteHub("定位优化").apply { openAllNetworks(); println(networksInfo()) }

    val f = dampingNewton(error, space)
    val init = space.ordinaryField.expressions.mapValues { Constant(1.0) }.let(::ExpressionVector)

    println(BEACONS.joinToString("\n", transform = Vector::rowView))
    remote.paintMap(init)

    recurrence(init to .0) { (p, _) -> f(p) }
        .take(2000)
        .withIndex()
        .firstOrLast { (i, t) ->
            val (p, s) = t
            remote.paintMap(p)
            (log2(s) * 20).toLong().takeIf { it > 0 }?.also { Thread.sleep(it) }
            println("$i\t$s")
            abs(s) < 2e-3
        }
        .value
        .first
        .also { println(it) }
}

fun RemoteHub.paintMap(field: ExpressionVector) {
    for (i in BEACONS.indices) {
        val x = field[Variable("x$i")]?.toDouble() ?: .0
        val y = field[Variable("y$i")]?.toDouble() ?: .0
        val z = field[Variable("z$i")]?.toDouble() ?: .0
        paint("标签$i", x, y, z)
    }
}
