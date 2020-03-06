package org.mechdancer.symbol.experiments

import org.mechdancer.algebra.function.vector.div
import org.mechdancer.algebra.function.vector.isZero
import org.mechdancer.algebra.function.vector.normalize
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.algebra.implement.vector.vector3DOfZero
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.*
import org.mechdancer.symbol.optimize.dampingNewton
import org.mechdancer.symbol.optimize.optimize
import kotlin.math.sqrt
import kotlin.random.Random

// 声速模型
private fun soundVelocity(t: Double) = 20.048 * sqrt(t)

private val collector = collector()
private val t by variable(collector) // 飞行时间
private val x by variable(collector) // 有效声音方向
private val y by variable(collector) //
private val z by variable(collector) //
private val space = collector.toSpace()

// 传声模型
private fun flightDuration(s: Vector3D, wind: Vector3D, c: Double): Double {
    val t0 = (s.length / c).takeIf { it > 0 } ?: return .0
    if (wind.isZero()) return t0

    val error = listOf(
        (x + wind.x) * t - s.x,
        (y + wind.y) * t - s.y,
        (z + wind.z) * t - s.z,
        sqrt(x * x + y * y + z * z) - c
    ).meanSquare()

    val (x0, y0, z0) = s / t0
    val init = point(t to t0, x to x0, y to y0, z to z0)
    val f = dampingNewton(error, space)
    return optimize(init, 200, 1e-9, f)[t]!!.toDouble()
}

fun main() {
    val remote = remoteHub("定位优化").apply {
        openAllNetworks()
        println(networksInfo())
    }

    while (true) {
        fun random() = Random.nextDouble(-1.0, 1.0)
        val s = vector3D(25, 0, 0)
        val wind = vector3D(random(), random(), random()).normalize() * (10 + 10 * random())
        val c = 340.0
        val t0 = flightDuration(s, vector3DOfZero(), c)
        val t = flightDuration(s, wind, c)

        remote.paint("关系", wind.length, (t - t0) * c)
    }
}
