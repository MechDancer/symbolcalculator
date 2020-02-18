package org.mechdancer.symbol

import org.mechdancer.algebra.core.rowView
import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.function.vector.minus
import org.mechdancer.algebra.function.vector.normalize
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector3D
import kotlin.math.abs

fun main() {
    val beacons = listOf(
        vector3D(0, 0, 0),
        vector3D(30, 0, 0),
        vector3D(30, 30, 0),
        vector3D(0, 30, 0))

    val mobile = vector3D(15, 20, -3)

    val x by variable
    val y by variable
    val z by variable

    // 热机
    repeat(10) {
        d(beacons.sumBy { distance(x, y, z, it, mobile euclid it).pow(2) }) / d(x)
    }

    val t0 = System.currentTimeMillis()

    val e = beacons.sumBy { distance(x, y, z, it, mobile euclid it).pow(2) }

    val dx = d(e) / d(x)
    val dy = d(e) / d(y)
    val dz = d(e) / d(z)

    println("求导算法耗时 = ${System.currentTimeMillis() - t0}ms")
    println("误差函数 = $e")
    println("de/dx = $dx")
    println("de/dy = $dy")
    println("de/dz = $dz")
    println()

    var p = vector3D(0, 0, -1)
    val pid = PIDLimiter(2E-4, 1E-5, 2E-5, 30.0)
    for (i in 1..1000) {
        val grad = sample(dx, dy, dz, x, y, z, p)
        val k = pid(grad.length)
        val step = grad.normalize() * k
        p -= step

        println("迭代次数 = $i")
        println("步长 = $k")
        println("步伐 = ${step.rowView()}")
        println("当前 = ${p.rowView()}")
        println("误差 = ${e.sample(x, y, z, p)}")
        println()

        if (abs(k) < 5E-3) break
    }
}

fun distance(
    x: Variable,
    y: Variable,
    z: Variable,

    target: Vector3D,
    measure: Double
) =
//    (x - target.x).pow(2) +
//    (y - target.y).pow(2) +
//    (z - target.z).pow(2) +
//    -measure.pow(2)
    sqrt((x - target.x).pow(2) +
         (y - target.y).pow(2) +
         (z - target.z).pow(2)) - measure

fun Expression.sample(
    x: Variable,
    y: Variable,
    z: Variable,

    current: Vector3D
) =
    (substitute {
        this[x] = current.x
        this[y] = current.y
        this[z] = current.z
    } as Constant).value

fun sample(
    dx: Expression,
    dy: Expression,
    dz: Expression,

    x: Variable,
    y: Variable,
    z: Variable,

    current: Vector3D
) = vector3D(
    dx.sample(x, y, z, current),
    dy.sample(x, y, z, current),
    dz.sample(x, y, z, current))

class PIDLimiter(
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
