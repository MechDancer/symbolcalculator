package org.mechdancer.symbol

import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector3D
import java.util.*
import kotlin.math.abs

private val beacons =
    listOf(vector3D(0, 0, 0),
           vector3D(30, 0, 0),
           vector3D(30, 30, 0),
           vector3D(0, 30, 0))

private val mobile =
    vector3D(15, 20, -3)

private val engine = Random()

fun measure(b: Vector3D, m: Vector3D) =
    (b euclid m) * (1 + 5E-3 * engine.nextGaussian()) + 1E-2 * engine.nextGaussian()

fun main() {
    val space = variables("x", "y", "z")
    val map = beacons.map { it.toField() to measure(it, mobile) }

    var t0 = System.currentTimeMillis()

    val e = map.sumBy { (beacon, measure) ->
        ((space.ordinaryField - beacon).length - measure) `^` 2
    }
    val gradField = space.gradientOf(e)

    println("求梯度耗时 = ${System.currentTimeMillis() - t0}ms")

    val actual = mobile.toField()
    var p = vector3D(1, 0, -1).toField()
    val pid = PIDLimiter(.4, .02, .02, 30.0)
    for (i in 1..200) {
        t0 = System.currentTimeMillis()

        val grad = gradField.substitute(p)
        val l = (grad.length as Constant).value
        val k = pid(l)
        val step = grad * (k / l)
        p -= step

        val t = System.currentTimeMillis() - t0

        println(p)
        println("迭代次数 = $i")
        println("迭代耗时 = ${t}ms")
        println("步长 = $k")
        println("损失 = ${e.substitute(p)}")
        println("误差 = ${(p - actual).length}")
        println()

        if (abs(k) < 5E-3) return
    }
}

private fun Vector3D.toField(): Field {
    val (x, y, z) = this
    return point("x" to x, "y" to y, "z" to z)
}

private class PIDLimiter(
    private val ka: Double,
    private val ki: Double,
    private val kd: Double,
    private val max: Double
) {
    private var last = .0
    private var sum = .0

    operator fun invoke(e: Double): Double {
        val dd = e - last
        last = e
        sum = .9 * sum + .1 * e
        val r = ka * e + ki * sum + kd * dd
        return when {
            r > +max -> +max
            r < -max -> -max
            else     -> r
        }
    }
}
