package org.mechdancer.symbol.system

import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.Vector2D
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.algebra.implement.vector.vector3DOfZero
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.networksInfo
import org.mechdancer.symbol.paintFrame3
import kotlin.math.*

/** 构造无序的二元组 */
fun <T : Comparable<T>> sortedPairOf(a: T, b: T) =
    if (a < b) a to b else b to a

/** 根据 4 点间具名的边长集，计算 4 点共面程度 */
fun <T : Comparable<T>> coplanarJudgeWithEdges(
    vararg edges: Pair<Pair<T, T>, Double>
) = coplanarJudgeWithEdges(edges.toMap())

/** 根据 4 点间具名的边长集，计算 4 点共面程度 */
fun <T : Comparable<T>> coplanarJudgeWithEdges(
    edges: Map<Pair<T, T>, Double>
): Double {
    val points = edges.keys.asSequence().flatMap { (p, q) -> sequenceOf(p, q) }.toSortedSet()
    require(edges.size == 6)
    require(points.size == 4)
    val (o, a, b, c) = points.toList()
    return coplanarJudgeWithEdges(
        edges.getValue(o to a),
        edges.getValue(o to b),
        edges.getValue(o to c),
        edges.getValue(a to b),
        edges.getValue(a to c),
        edges.getValue(b to c))
}

/** 根据 4 点间不具名的边长集，计算 4 点共面程度 */
fun coplanarJudgeWithEdges(vararg edges: Double): Double {
    require(edges.size == 6)

    operator fun DoubleArray.component6() = this[5]

    // 海伦公式
    fun heron(a: Double, b: Double, c: Double) =
        ((a + b + c) / 2).let { p -> sqrt(p * (p - a) * (p - b) * (p - c)) }

    val (oa, ob, oc, ab, ac, bc) = edges
    val b = (heron(oa, ob, ab) * 2 / oa).let { x -> Vector2D(+x, sqrt(ob * ob - x * x)) }
    val c = (heron(oa, oc, ac) * 2 / oa).let { x -> Vector2D(-x, sqrt(oc * oc - x * x)) }
    return min(abs((b euclid c) - bc), abs((b * Vector2D(-1.0, 1.0) euclid c) - bc)) / bc
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
            val frame = listOf(listOf(o, b, a, o, c, a), listOf(b, c))
            remote.paintFrame3("frame", frame)
            if (coplanarJudgeWithEdges(
                    "a" to "o" to (a euclid o),
                    "b" to "o" to (b euclid o),
                    "c" to "o" to (c euclid o),
                    "a" to "b" to (a euclid b),
                    "a" to "c" to (a euclid c),
                    "b" to "c" to (b euclid c)) < 5e-4)
                remote.paintFrame3("static", frame)
            Thread.sleep(10)
        }
}
