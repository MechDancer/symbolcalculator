package org.mechdancer.symbol.experiments

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.core.rowView
import org.mechdancer.algebra.doubleEquals
import org.mechdancer.algebra.function.matrix.times
import org.mechdancer.algebra.function.vector.*
import org.mechdancer.algebra.implement.matrix.builder.matrix
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.to3D
import org.mechdancer.algebra.implement.vector.toListVector
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.networksInfo
import org.mechdancer.symbol.paint
import org.mechdancer.symbol.paintFrame3
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val maxMeasure = 15.0
private val interval = maxMeasure / sqrt(2.0) * .99
//private val upperRange = (.8 * interval).roundToInt()
//private val zList = listOf(-3.2, -2.4, -1.6)
//
//// 地图
//private val beacons =
//    (matrix {
//        row(-1, -1, 0)
//        row(-1, +1, 0)
//        row(+1, +1, 0)
//        row(+1, -1, 0)
//    } * (interval / 2)).rows.map(Vector::to3D)

private val upperRange = (.8 * interval).roundToInt()
private val zList = listOf(-3.2, -2.4, -1.6)

// 地图
private val beacons =
    (matrix {
        row(-2, -1, 0)
        row(-2, +1, 0)
        row(+0, +1, 0)
        row(+0, -1, 0)
        row(+2, -1, 0)
        row(+2, +1, 0)
    } * (interval / 2)).rows.map(Vector::to3D)

// 噪声

private val engine = java.util.Random()
private fun gaussian(sigma: Double) = sigma * engine.nextGaussian()

private fun measure(d: Double) = d * (1 + gaussian(1e-3)) + gaussian(5e-3)
private fun deploy(p: Vector3D) = p + vector3D(gaussian(.04), gaussian(.04), gaussian(.04))

fun main() {
    val remote = remoteHub("定位优化").apply {
        openAllNetworks()
        println(networksInfo())
    }
    val locator = Locator(beacons)

    for (x in 0..upperRange * 2)
        for (y in if (x % 2 == 0) x / 2..upperRange else upperRange downTo x / 2)
            for (z in if (y % 2 == 0) zList else zList.asReversed()) {
                val mobile = vector3D(x, y, z)
                val errors = (1..20).map {
                    val map = beacons.map(::deploy)
                    val measures = map.map { p -> measure(p euclid mobile).takeIf { it < maxMeasure } ?: -1.0 }
                    val result = locator(measures)!!

                    with(remote) {
                        paintFrame3("目标", map.filterIndexed { i, _ -> measures[i] > 0 }.map { listOf(mobile, it) })
                        paintFrame3("定位", beacons.filterIndexed { i, _ -> measures[i] > 0 }.map { listOf(result, it) })
                    }

                    result - mobile
                }
                val (a, e, d) = errors.statistic()
                remote.paint("路径", x, y, z)
                remote.paint("均值", x + e.x, y + e.y, z + e.z)
                println("${mobile.rowView()} -> ${a.rowView()} | ${e.rowView()} | ${d.rowView()}")
            }
}

private fun Collection<Vector3D>.statistic(): Triple<Vector, Vector, Vector> {
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
    val sigma = square.toList().zip(expect.toList()) { a, b ->
        (a / size - b * b).takeUnless { doubleEquals(it, .0) }?.let(::sqrt) ?: .0
    }.toListVector()
    return Triple(abs, expect, sigma)
}
