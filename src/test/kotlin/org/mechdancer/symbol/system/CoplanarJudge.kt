package org.mechdancer.symbol.system

import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.Vector2D
import org.mechdancer.algebra.implement.vector.vector2D
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.algebra.implement.vector.vector3DOfZero
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.networksInfo
import org.mechdancer.symbol.paintFrame3
import kotlin.math.*

private fun heron(a: Double, b: Double, c: Double): Double {
    val p = (a + b + c) / 2
    return sqrt(p * (p - a) * (p - b) * (p - c))
}

private val mirror = vector2D(-1, 1)

fun coplanarJudgeWithEdges(list: List<Double>, epsilon: Double): Boolean {
    operator fun <T> List<T>.component6() = this[5]

    require(list.size == 6)
    val (oa, ob, oc, ab, ac, bc) = list
    val sOAB = heron(oa, ob, ab) * 2
    val sOAC = heron(oa, oc, ac) * 2
    val b = (sOAB / oa).let { x -> Vector2D(+x, sqrt(ob * ob - x * x)) }
    val c = (sOAC / oa).let { x -> Vector2D(-x, sqrt(oc * oc - x * x)) }
    return abs((b euclid c) - bc) / bc < epsilon || abs((b * mirror euclid c) - bc) / bc < epsilon
}

fun main() {
    val remote = remoteHub("四点共面").apply {
        openAllNetworks()
        println(networksInfo())
    }

    val o = vector3DOfZero()
    val a = vector3D(0, 20, 0)
    val b = vector3D(-15, 10, 0)
    while (true)
        repeat(1000) { i ->
            val theta = i * 2 * PI / 1000
            val c = vector3D(cos(theta), 2, sin(theta)) * 10
            val frame = listOf(listOf(o, b, a, o, c, a))
            remote.paintFrame3("frame", frame)
            if (coplanarJudgeWithEdges(listOf(a.length, b.length, c.length, a euclid b, a euclid c, b euclid c), 1e-3))
                remote.paintFrame3("static", frame)
            Thread.sleep(10)
        }
}
