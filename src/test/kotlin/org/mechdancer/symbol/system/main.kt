package org.mechdancer.symbol.system

import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.function.vector.times
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
private const val interval = maxMeasure * .95

private val engine = java.util.Random()
private fun gaussian(sigma: Double) = sigma * engine.nextGaussian()
private fun deploy(p: Vector3D) = p + vector3D(gaussian(.1), gaussian(.1), gaussian(.1))

val shape = vector3D(.5, sqrt(3.0) / 2, 0) * interval
private val beacons =
    sequence {
        for (i in 0 until 6)
            yield(vector3D(i / 2, i % 2, 0) * shape)
    }.map(::deploy).toList()

private val beaconCount = beacons.size

fun main() {
    val world = SimulationWorld(
        beacons.mapIndexed { i, p -> Beacon(i) to p }.toMap(),
        (maxMeasure * 1000).toLong() / 330)
    val system = LocatingSystem(maxMeasure)
    val remote = remoteHub("sqrt(4)").apply {
        openAllNetworks()
        println(networksInfo())
    }

    for ((pair, l) in world.preMeasures()) {
        val (a, b) = pair
        system[a, b, -1L] = l
    }

    system.painter = {
        remote.paintFrame3("步骤", it.toPoints())
    }

    val grid = world.edges().map { it.toList().map { it.beacon.id } }
    thread(isDaemon = true) {
        while (true) {
            remote.paintFrame3("实际地图", grid.map { it.map(beacons::get) })
            Thread.sleep(2000L)
        }
    }
    println("optimize in ${measureTimeMillis { system.optimize() }}ms")

    val mobile = Beacon(beaconCount)
    val steps = 200
    val dx = shape.x * 2 / steps
    val dy = shape.y / steps
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
        remote.paint("历史", result)
        print("step $i: ")
        print(m euclid result)
        print("\t")
        println(vector2D(m.x, m.y) euclid vector2D(result.x, result.y))
    }
}

private fun Map<Beacon, Vector3D>.toPoints(): List<List<Vector3D>> {
    val tf = entries.groupBy { (key, _) -> key.id in beacons.indices }
        .getValue(true)
        .map { (key, p) -> beacons[key.id] to p }
        .toTransformationWithSVD(1e-8)
    return listOf(map { (_, p) -> (tf * p).to3D() })
}
