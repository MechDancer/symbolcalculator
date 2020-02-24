@file:Suppress("ObjectPropertyName", "NonAsciiCharacters")

package org.mechdancer.symbol.experiments

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.core.rowView
import org.mechdancer.algebra.doubleEquals
import org.mechdancer.algebra.function.vector.div
import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.function.vector.minus
import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.toListVector
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.*
import org.mechdancer.symbol.linear.ExpressionVector
import kotlin.math.abs
import kotlin.math.sqrt

private fun Vector3D.toPoint() =
    ExpressionVector(mapOf(Variable("x") to Constant(x),
                           Variable("y") to Constant(y),
                           Variable("z") to Constant(z)))

private const val interval = 15

// 地图
private val BEACONS = listOf(
    vector3D(-interval, -interval, 0),
    vector3D(-interval, 0, 0),
    vector3D(-interval, interval, 0),
    vector3D(0, interval, 0),
    vector3D(interval, interval, 0),
    vector3D(interval, 0, 0),
    vector3D(interval, -interval, 0),
    vector3D(0, -interval, 0))

// 测量函数

private val engine = java.util.Random()
private fun measure(b: Vector3D, m: Vector3D) =
    (b euclid m) //* (1 + 1e-3 * engine.nextGaussian()) + 5e-3 * engine.nextGaussian()

fun main() {
    val remote = remoteHub("定位优化").apply {
        openAllNetworks()
        println(networksInfo())
    }
    val space by variableSpace("x", "y", "z")
    val struct = BEACONS.map { (space.ordinaryField - it.toPoint()).length() }.withIndex()

    val vx = Variable("x")
    val vy = Variable("y")
    val vz = Variable("z")

    for (x in 0..30) for (y in x..30) for (z in -5..-1) {
        // 移动标签
        val mobile = vector3D(x, y, z)
        val errors = (1..50).map {
            // 测量
            val map = BEACONS.map { measure(it, mobile) }
            // FIXME 确定方程增益 有反效果？为什么
            // val max = map.max()!! + 1
            // val a = map.map { max - it }
            // val f = newton(struct.sumBy { (i, e) -> ((e - map[i]) `^` 2) } / BEACONS.size, space)
            // 求损失函数
            val error = struct.sumBy { (i, e) -> ((e - map[i]) `^` 2) } / BEACONS.size
            val f = dampingNewton(error, space)
            //求解
            val result =
                recurrence(vector3D(0, 0, -10).toPoint() to .0) { (p, _) -> f(p) }
                    .take(100)
//                    .onEach { (p, _) -> remote.paint(p); Thread.sleep(1000) }
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

fun Collection<Vector>.statistic(): Triple<Vector, Vector, Vector> {
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
