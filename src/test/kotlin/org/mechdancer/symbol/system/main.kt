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
import kotlin.math.*
import kotlin.system.measureTimeMillis

private const val maxMeasure = 30.0
private const val interval = maxMeasure * .95

private val engine = java.util.Random()
private fun gaussian(sigma: Double) = sigma * engine.nextGaussian()
private fun deploy(p: Vector3D) = p + vector3D(gaussian(.1), gaussian(.1), gaussian(.1))

private val lx = .5
private val ly = sqrt(1 - lx * lx)

private val shape = vector3D(lx, ly, 0) * interval + vector3D(0, 0, 1)
private val beacons = (0 until 6).map { deploy(vector3D(it / 2, it % 2, 0) * shape) }

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

    val mobile = Beacon(beacons.size)
    val steps = 400
    val dTheta = 4 * PI / steps
    for (i in 0 until steps) {
        val time = System.currentTimeMillis()
        val position = mobile.move(time)

        val theta = i * dTheta
        val m = (vector3D(cos(theta), .5 * sin(theta), 0) * (1 - i.toDouble() / steps) +
                 vector3D(1, .5, sin(8 * theta) * .5 - 1.5)) * shape
        for ((pair, l) in world.measure(position, m)) {
            val (a, b) = pair
            system[a, b, time] = l
        }
        val part = system[mobile].toPoints()
        val result = part.single().last().run { copy(z = -abs(z)) }
        remote.paint("目标", m)
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
