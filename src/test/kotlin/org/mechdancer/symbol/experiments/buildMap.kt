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
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.VariableSpace
import org.mechdancer.symbol.optimize.fastestBatchGD
import org.mechdancer.symbol.optimize.recurrence
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
            yield(vector3D(l * i, interval * .3, -2))
            yield(vector3D(l * i, interval * .7, -2))
        }
    }.map(::deploy).toList()

fun main() {
    val space = beacons
        .indices
        .flatMap { i -> variables("x$i", "y$i", "z$i").variables }
        .toSet()
        .let(::VariableSpace)
    val edges = sequence {
        for (i in 0 until beaconCount) for (j in i + 1 until beacons.size) {
            if (beacons[i] euclid beacons[j] < maxMeasure) yield(listOf(i, j))
        }
    }.toList()
    val errors =
        edges.map { (i, j) ->
            ((variable3D(i) - variable3D(j)).length() - measure(beacons[i] euclid beacons[j]) `^` 2) / (2 * edges.size)
        }

    val init = space.ordinaryField.map {
        Constant(
            if ((it as Variable).name.drop(1).toInt() < beaconCount)
                gaussian(maxMeasure)
            else
                gaussian(maxMeasure) - 5 * maxMeasure)
    }

    println(beacons.joinToString("\n", transform = Vector::rowView))
    val remote = remoteHub("定位优化").apply {
        openAllNetworks()
        println(networksInfo())
        paintMap(edges, init)
    }
    val f = fastestBatchGD(errors.sum(), space)
    recurrence(init to .0) { (p, _) -> f(p) }
        .onEach { (p, s) -> remote.paintMap(edges, p); println(s) }
        .last()
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

private fun RemoteHub.paintMap(
    edges: List<List<Int>>,
    field: ExpressionVector
) {
    val points = field.toPoints()
    paintFrame3("连线", edges.map { list -> list.map { points[it] } })
}
