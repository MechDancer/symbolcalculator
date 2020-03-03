package org.mechdancer.symbol.experiments

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.core.rowView
import org.mechdancer.algebra.doubleEquals
import org.mechdancer.algebra.function.vector.*
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.toListVector
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.optimize.dampingNewton
import kotlin.math.abs
import kotlin.math.sqrt

private fun Vector3D.toPoint() =
    ExpressionVector(mapOf(Variable("x") to Constant(x),
                           Variable("y") to Constant(y),
                           Variable("z") to Constant(z)))

// 测量函数

private val engine = java.util.Random()
private fun deploy() = vector3D(engine.nextGaussian(), engine.nextGaussian(), engine.nextGaussian()) * 2e-2

private const val maxMeasure = 30.0
private val interval = maxMeasure / 2 / sqrt(2.0)

// 地图
private val BEACONS = listOf(
    vector3D(-interval, 0, 0),
    vector3D(0, interval, 0),
    vector3D(interval, 0, 0),
    vector3D(0, -interval, 0))

fun main() {
    val remote = remoteHub("定位优化").apply {
        openAllNetworks()
        println(networksInfo())
    }
    val space by variableSpace("x", "y", "z")
    val struct = BEACONS.associateWith { (space.ordinaryField - it.toPoint()).length() }

    val vx = Variable("x")
    val vy = Variable("y")
    val vz = Variable("z")

    val upperRange = interval.toInt() + 1
    for (x in 0..upperRange) for (y in x..upperRange) for (z in -3..-1) {
        // 移动标签
        val mobile = vector3D(x, y, z)
        val errors = (1..50).map {
            // 测量
            val map = BEACONS
                .associateWith { mobile euclid it + deploy() }
                .filterValues { it < maxMeasure }
            // 求损失函数
            val error = map.entries.sumBy { (b, e) -> e - struct.getValue(b) `^` 2 } / map.size
            val f = dampingNewton(error, space)
            //求解
            val result =
                recurrence(vector3D(0, 0, -10).toPoint() to .0) { (p, _) -> f(p) }
                    .take(100)
                    .firstOrLast { (_, s) -> abs(s) < 5e-4 }
                    .first
                    .let { result ->
                        val zz = result[Variable("z")]!!.toDouble()
                        result.takeIf { zz > -.1 }
                            ?.expressions
                            ?.toMutableMap()
                            ?.also { it[Variable("z")] = Constant(-zz) }
                            ?.let(::ExpressionVector)
                        ?: result
                    }
            remote.paint(result - mobile.toPoint())
            result.let { vector3D(it[vx]!!.toDouble(), it[vy]!!.toDouble(), it[vz]!!.toDouble()) } - mobile
        }
        val (a, e, d) = errors.statistic()
        println("${mobile.rowView()} -> ${a.rowView()} | ${e.rowView()} | ${d.rowView()}")
    }
}

private fun Collection<Vector>.statistic(): Triple<Vector, Vector, Vector> {
    var abs = first().toList().map(::abs).toListVector()
    var expect = first()
    var square = first().toList().map { it * it }.toListVector()

    for (item in this.drop(1)) {
        abs += item.toList().map(::abs).toListVector()
        expect += item
        square += item.toList().map { it * it }.toListVector()
    }
    abs /= size
    expect /= size
    square = square.toList().zip(expect.toList()) { a, b ->
        (a / size - b * b).takeUnless { doubleEquals(it, .0) }?.let(::sqrt) ?: .0
    }.toListVector()
    return Triple(abs, expect, square)
}
