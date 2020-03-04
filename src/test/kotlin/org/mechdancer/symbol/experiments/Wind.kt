package org.mechdancer.symbol.experiments

import org.mechdancer.algebra.function.vector.minus
import org.mechdancer.algebra.function.vector.normalize
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.listVectorOf
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.algebra.implement.vector.vector3DOfZero
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.*
import org.mechdancer.symbol.optimize.dampingNewton
import org.mechdancer.symbol.optimize.firstOrLast
import org.mechdancer.symbol.optimize.recurrence
import kotlin.random.Random

fun main() {
    val remote = remoteHub("定位优化").apply {
        openAllNetworks()
        println(networksInfo())
    }

    println("风速\t测距误差")
    while (true) {
        val b0 = vector3DOfZero()
        val b1 = vector3D(25, 0, 0)

        fun random() = Random.nextDouble(-1.0, 1.0)

        val vw = vector3D(random(), random(), random()).normalize() * (10 + 10 * random())
        val vs = 340

        val s = b1 - b0

        val t by variable
        val vx by variable
        val vy by variable
        val vz by variable
        val space = variables("t", "vx", "vy", "vz")

        val error = listOf(
            (vx + vw.x) * t - s.x,
            (vy + vw.y) * t - s.y,
            (vz + vw.z) * t - s.z,
            sqrt(vx * vx + vy * vy + vz * vz) - vs)
            .run { sumBy { it `^` 2 } / (2 * size) }
        val f = dampingNewton(error, space)
        val init = (s.normalize() * vs).let { (x, y, z) ->
            space.order(listVectorOf(s.length / vs, x, y, z))
        }
        recurrence(init to .0) { (x, _) -> f(x) }
            .take(200)
            .firstOrLast { (_, s) -> s < 1e-9 }
            .let { (r, _) ->
                val a = vw.length
                val b = (r[t]!! - init[t]!!).toDouble() * vs
                remote.paint("关系", a, b)
                println("$a\t$b")
            }
    }
}
