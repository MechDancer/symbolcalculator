package org.mechdancer.symbol.experiments

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.core.rowView
import org.mechdancer.algebra.doubleEquals
import org.mechdancer.algebra.function.vector.*
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.toListVector
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.networksInfo
import org.mechdancer.symbol.paint
import org.mechdancer.symbol.paintFrame3
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val maxMeasure = 30.0
private val interval = maxMeasure / 2 / sqrt(2.0) * .99

// 地图
private val beacons = listOf(
    vector3D(-interval, -interval, 0),
    vector3D(-interval, interval, 0),
    vector3D(interval, interval, 0),
    vector3D(interval, -interval, 0))

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

    val upperRange = (1.5 * interval).roundToInt()
    val zList = listOf(-2.6, -2.3, -2.0)
    for (x in 0..upperRange)
        for (y in if (x % 2 == 0) x..upperRange else upperRange downTo x)
            for (z in if ((y - x) % 2 == 0) zList else zList.asReversed()) {
                val mobile = vector3D(x, y, z)
                val errors = (1..100).map {
                    val map = beacons.map(::deploy)
                    val result = map
                        .map { p -> measure(p euclid mobile).takeIf { it < maxMeasure } ?: -1.0 }
                        .let(locator::invoke)

                    remote.paintFrame3("目标", map.map { listOf(mobile, it) })
                    remote.paintFrame3("定位", beacons.map { listOf(result, it) })

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
