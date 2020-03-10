package org.mechdancer.symbol.system

import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.networksInfo
import org.mechdancer.symbol.paintFrame3
import kotlin.math.sqrt

private const val maxMeasure = 30.0
private val interval = maxMeasure / sqrt(2.0) * .9

private val engine = java.util.Random()
private fun gaussian(sigma: Double) = sigma * engine.nextGaussian()
private fun deploy(p: Vector3D) = p + vector3D(gaussian(.1), gaussian(.1), gaussian(.1))

private const val beaconCount = 6

private val beacons =
    sequence {
        for (i in 0 until beaconCount / 2) {
            yield(vector3D(i * interval, 0, 0))
            yield(vector3D(i * interval, interval, 0))
        }
    }.map(::deploy).toList()

private val grid = sequence {
    (0 until beaconCount / 2)
        .flatMap {
            if (it % 2 == 0)
                listOf(it * 2, it * 2 + 1)
            else
                listOf(it * 2 + 1, it * 2)
        }
        .also { yield(it) }
    for (i in 0 until beaconCount / 2 - 1)
        yield(if (i % 2 == 0)
                  listOf(i * 2, i * 2 + 2)
              else
                  listOf(i * 2 + 1, i * 2 + 3))
}.toList()

fun main() {
    val world = SimulationWorld(
        beacons.mapIndexed { i, p -> Beacon(i) to p }.toMap(),
        30_000L / 330)
    val system = LocatingSystem()
    val remote = remoteHub("定位优化").apply {
        openAllNetworks()
        println(networksInfo())
    }
    while (true) {
        remote.paintFrame3("实际地图", grid.map { it.map(beacons::get) })
        Thread.sleep(2000L)
    }
}
