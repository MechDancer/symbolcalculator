package org.mechdancer.symbol.experiments

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.core.rowView
import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.remote.presets.RemoteHub
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.FunctionExpression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.VariableSpace
import org.mechdancer.symbol.optimize.fastestBatchGD
import org.mechdancer.symbol.optimize.recurrence
import org.mechdancer.symbol.optimize.stochasticGD
import kotlin.math.sqrt

// 地图

private const val maxMeasure = 30.0
private val interval = maxMeasure / sqrt(2.0) * .9

private val engine = java.util.Random()
private fun gaussian(sigma: Double) = sigma * engine.nextGaussian()
private val multiplier = 1e-3 * engine.nextGaussian() + 1
private fun measure(d: Double) = d * multiplier + 5e-3 * engine.nextGaussian()
private fun deploy(p: Vector3D) = p + vector3D(gaussian(.1), gaussian(.1), gaussian(.1))

private const val beaconCount = 6
private const val mobileCount = 3

private val beacons =
    sequence {
        for (i in 0 until beaconCount / 2) {
            yield(vector3D(i * interval, 0, 0))
            yield(vector3D(i * interval, interval, 0))
        }
        val l = 2 * interval / (mobileCount + 1)
        for (i in 1..mobileCount) {
            yield(vector3D(l * i, interval * .1, -2))
            yield(vector3D(l * i, interval * .9, -2))
        }
    }.map(::deploy).toList()

fun main() {
    val measures = beacons
        .map { a -> beacons.map { b -> measure(a euclid b) } }
    val space = beacons.mapIndexed { i, _ -> variables("x$i", "y$i", "z$i").variables }
        .flatten()
        .toSet()
        .let(::VariableSpace)
    val samples = measures
        .mapIndexed { i, distances ->
            distances.mapIndexed { j, distance ->
                if (distance < maxMeasure)
                    (variable3D(i) - variable3D(j)).length() - distance
                else
                    Constant.`0`
            }
        }
        .flatten()
        .filterIsInstance<FunctionExpression>()

    val f = stochasticGD(samples) { fastestBatchGD(it, space) }
    val init = space.ordinaryField.map {
        Constant(
            if ((it as Variable).name.drop(1).toInt() < beaconCount)
                gaussian(maxMeasure)
            else
                gaussian(maxMeasure) - 20 * maxMeasure)
    }

    println(beacons.joinToString("\n", transform = Vector::rowView))
    val remote = remoteHub("定位优化").apply {
        openAllNetworks()
        println(networksInfo())
        paintFrame3("目标", edges.map { list -> list.map { beacons[it] } })
        paintFrame3("其他目标", listOf(beacons.takeLast(mobileCount * 2)))
        paintMap(init)
    }

    recurrence(init to .0) { (p, _) -> f(p) }
        .onEach { (p, _) -> remote.paintMap(p) }
        .last()
        .first
        .also(::printError)
}

private fun variable3D(i: Int) =
    listOf("x", "y", "z")
        .associate { Variable(it) to Variable("$it$i") }
        .let(::ExpressionVector)

private fun ExpressionVector.toPoints() =
    beacons.indices.map { i ->
        val x = this[Variable("x$i")]?.toDouble() ?: .0
        val y = this[Variable("y$i")]?.toDouble() ?: .0
        val z = this[Variable("z$i")]?.toDouble() ?: .0
        vector3D(x, y, z)
    }

private val edges = listOf(
    listOf(0, 1),
    listOf(0, 2, 3, 1),
    listOf(2, 4, 5, 3))

private fun RemoteHub.paintMap(field: ExpressionVector) {
    val points = field.toPoints()
    paintFrame3("连线", edges.map { list -> list.map { points[it] } })
    paintFrame3("其他", listOf(points.takeLast(mobileCount * 2)))
}

private fun printError(field: ExpressionVector) =
    field.toPoints().forEachIndexed { i, v -> println("标签$i: ${v euclid beacons[i]}") }
