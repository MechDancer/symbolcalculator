@file:Suppress("ObjectPropertyName", "NonAsciiCharacters")

package org.mechdancer.symbol.experiments

import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.*
import org.mechdancer.symbol.linear.ExpressionVector
import java.util.*
import kotlin.math.abs

private fun Vector3D.toPoint() =
    ExpressionVector(mapOf(Variable("x") to Constant(x),
                           Variable("y") to Constant(y),
                           Variable("z") to Constant(z)))

// 地图
private val BEACONS = listOf(
    vector3D(0, 0, 0),
    vector3D(0, 15, 0),
    vector3D(0, 30, 0),
    vector3D(15, 30, 0),
    vector3D(30, 30, 0),
    vector3D(30, 15, 0),
    vector3D(30, 0, 0),
    vector3D(15, 0, 0))

// 移动标签
private val mobile =
    vector3D(15, 10, -1)

// 测量函数

private val engine = Random()
private fun measure(b: Vector3D, m: Vector3D) =
    (b euclid m) * (1 + 1e-3 * engine.nextGaussian()) + 5e-3 * engine.nextGaussian()

fun main() {
    val remote = remoteHub("定位优化").apply {
        openAllNetworks()
        println(networksInfo())
    }
    val space by variableSpace("x", "y", "z")
    val struct = BEACONS.map { (space.ordinaryField - it.toPoint()).length() }.withIndex()
    val init = vector3D(15, 15, -3).toPoint()
    while (true) {
        val map = BEACONS.map { measure(it, mobile) }
        val max = map.max()!! + 1
        val kkk = map.map { max - it }
        val k = kkk.sum()
        val t0 = System.currentTimeMillis()
        val f = newton(struct.sumBy { (i, e) -> ((e - map[i]) `^` 2) * kkk[i] } / k, space)
        val t1 = System.currentTimeMillis()
        val result =
            recurrence(init to .0) { (p, _) -> f(p) }
                .take(200)
                .firstOrLast { (_, s) -> abs(s) < 5E-4 }
                .first
                .let { result ->
                    val z = result[Variable("z")]!!.toDouble()
                    result.takeIf { z > -.1 }
                        ?.expressions
                        ?.toMutableMap()
                        ?.also { it[Variable("z")] = Constant(-z) }
                        ?.let(::ExpressionVector)
                    ?: result
                }
        val t2 = System.currentTimeMillis()
        remote.paint(result - mobile.toPoint())
        println("总耗时 = 求梯度 + 迭代 = ${t1 - t0}ms + ${t2 - t1}ms = ${t2 - t0}ms")
    }
}
