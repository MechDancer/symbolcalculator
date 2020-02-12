package org.mechdancer.symbol

import org.mechdancer.algebra.core.rowView
import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.function.vector.minus
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.algebra.implement.vector.vector3DOfZero
import org.mechdancer.symbol.DExpression.Companion.d
import kotlin.math.pow

fun main() {
    val beacons = listOf(
        vector3D(0, 0, 0),
        vector3D(30, 0, 0),
        vector3D(30, 30, 0),
        vector3D(0, 30, 0),
        vector3D(0, 0, -10),
        vector3D(30, 0, -10),
        vector3D(30, 30, -10),
        vector3D(0, 30, -10))

    val mobile = vector3D(15, 20, -2)

    val x by variable
    val y by variable
    val z by variable

    val e = beacons.sumBy { distance(x, y, z, it, mobile euclid it).pow(2) }
    val dx = d(e) / d(x)
    val dy = d(e) / d(y)
    val dz = d(e) / d(z)

    println("误差函数 = $e")
    println("de/dx = $dx")
    println("de/dy = $dy")
    println("de/dz = $dz")
    println()

    var p = vector3DOfZero()
    for (i in 1..1000) {
        val grad = sample(dx, dy, dz, x, y, z, p) * 5E-5
        if (grad.length < 0.01) break
        p -= grad
        println("迭代次数 = $i")
        println("梯度 = ${grad.rowView()}")
        println("当前 = ${p.rowView()}")
        println("误差 = ${e.sample(x, y, z, p)}")
        println()
    }
}

fun distance(
    x: Variable,
    y: Variable,
    z: Variable,

    target: Vector3D,
    measure: Double
) =
    (x - target.x).pow(2) +
    (y - target.y).pow(2) +
    (z - target.z).pow(2) +
    -measure.pow(2)

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
