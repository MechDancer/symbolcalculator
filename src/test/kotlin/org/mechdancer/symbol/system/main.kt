package org.mechdancer.symbol.system

import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.to3D
import org.mechdancer.algebra.implement.vector.vector2D
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.geometry.transformation.toTransformationWithSVD
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.networksInfo
import org.mechdancer.symbol.paint
import org.mechdancer.symbol.paintFrame3
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

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
        (maxMeasure * 1000).toLong() / 330)
    val system = LocatingSystem()
    val remote = remoteHub("sqrt(5)").apply {
        openAllNetworks()
        println(networksInfo())
    }
    thread(isDaemon = true) {
        while (true) {
            remote.paintFrame3("实际地图", grid.map { it.map(beacons::get) })
            Thread.sleep(2000L)
        }
    }
    for ((pair, l) in world.preMeasures()) {
        val (a, b) = pair
        system[a, b, -1L] = l
    }
    println("optimize in ${measureTimeMillis { system.optimize() }}ms")
    remote.paintFrame3("初始化", system.newest().toPoints())

    val mobile = Beacon(beaconCount)
    val steps = 200
    val dx = 2.0 * interval / steps
    val dy = 1.0 * interval / steps
    for (i in 0 until steps) {
        val time = System.currentTimeMillis()
        val position = mobile.move(time)
        val m = vector3D(i * dx, i * dy, -1.5)
        for ((pair, l) in world.measure(position, m)) {
            val (a, b) = pair
            system[a, b, time] = l
        }
        val part = system[mobile].toPoints()
        val result = part.single().last().run { copy(z = -abs(z)) }
        remote.paintFrame3("优化", part)
        remote.paint("历史", result)
        print("step $i: ")
        print(m euclid result)
        print("\t")
        println(vector2D(m.x, m.y) euclid vector2D(result.x, result.y))
    }
}

fun Map<Beacon, Vector3D>.toPoints(): List<List<Vector3D>> {
    val tf = entries.groupBy { (key, _) -> key.id in beacons.indices }
        .getValue(true)
        .map { (key, p) -> beacons[key.id] to p }
        .toTransformationWithSVD(1e-8)
    return listOf(map { (_, p) -> (tf * p).to3D() })
}
