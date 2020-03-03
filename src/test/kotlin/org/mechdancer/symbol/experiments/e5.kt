package org.mechdancer.symbol.experiments

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.core.rowView
import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.remote.presets.RemoteHub
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.FunctionExpression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.VariableSpace
import org.mechdancer.symbol.optimize.sgd
import kotlin.math.sqrt

// 地图

private val engine = java.util.Random()
private const val maxMeasure = 30.0
private val interval = maxMeasure / sqrt(5.0) * .99

private val BEACONS = listOf(
    vector3D(0, 0, 0),
    vector3D(interval, 0, 0),
    vector3D(interval, interval, 0),
    vector3D(0, interval, 0),
    vector3D(2 * interval, 0, 0),
    vector3D(2 * interval, interval, 0))

private val ZEROS = variables("x0", "y0", "z0")

private fun measure(d: Double) = d * (1 + 1e-3 * engine.nextGaussian()) + 5e-3 * engine.nextGaussian()

fun main() {
    val measures = BEACONS.map { a -> BEACONS.map { b -> measure(a euclid b) } }
    val space = BEACONS.mapIndexed { i, _ -> variables("x$i", "y$i", "z$i").variables }
                    .flatten()
                    .toSet()
                    .let(::VariableSpace) - ZEROS
    val samples = measures
        .mapIndexed { i, distances ->
            distances.mapIndexed { j, distance ->
                if (distance < maxMeasure)
                    sqrt((Variable("x$i") - Variable("x$j") `^` 2) +
                         (Variable("y$i") - Variable("y$j") `^` 2) +
                         (Variable("z$i") - Variable("z$j") `^` 2)) - distance
                else
                    Constant.`0`
            }
        }
        .flatten()
        .map { it.substitute { for (v in ZEROS.variables) this[v] = 0 } }
        .filterIsInstance<FunctionExpression>()

    val f = sgd(samples, space) { .01 * it }
    var i = 1
    val init = space.ordinaryField.expressions.mapValues { Constant(++i * 3.0) }.let(::ExpressionVector)

    println(BEACONS.joinToString("\n", transform = Vector::rowView))
    val remote = remoteHub("定位优化").apply { openAllNetworks(); println(networksInfo()) }
    remote.paintFrame3("目标", edges.map { list -> list.map { BEACONS[it] } })
    remote.paintMap(init)

    recurrence(init to .0) { (p, _) -> f(p) }
        .onEach { (p, _) -> remote.paintMap(p) }
        .last()
        .first
        .also(::printError)
}

private fun ExpressionVector.toPoints() =
    BEACONS.indices.map { i ->
        val x = this[Variable("x$i")]?.toDouble() ?: .0
        val y = this[Variable("y$i")]?.toDouble() ?: .0
        val z = this[Variable("z$i")]?.toDouble() ?: .0
        vector3D(x, y, z)
    }

private val edges = listOf(
    listOf(0, 1, 2, 3, 0),
    listOf(1, 4, 5, 2))

private fun RemoteHub.paintMap(field: ExpressionVector) {
    val points = field.toPoints()
    paintFrame3("连线", edges.map { list -> list.map { points[it] } })
}

private fun printError(field: ExpressionVector) =
    field.toPoints().forEachIndexed { i, v -> println("标签$i: ${v euclid BEACONS[i]}") }
