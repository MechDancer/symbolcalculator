package org.mechdancer.symbol.experiments.system

import org.mechdancer.algebra.function.vector.*
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.to3D
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.algebra.implement.vector.vector3DOfZero
import org.mechdancer.geometry.transformation.toTransformationWithSVD
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.networksInfo
import org.mechdancer.symbol.paint
import org.mechdancer.symbol.paintFrame3
import org.mechdancer.symbol.system.Beacon
import org.mechdancer.symbol.system.LocatingSystem
import org.mechdancer.symbol.system.Position
import org.mechdancer.symbol.system.WorldBuilderDsl.Companion.world
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.measureTimeMillis

// 6 个固定标签组成矩形，1 个移动标签沿矩形对角线运动

private const val maxMeasure = 30.0
private const val interval = maxMeasure * .75
private const val edgeCount = 6

private val beacons = sequence {
    yield(vector3DOfZero())
    for (i in 0 until edgeCount) {
        val theta = 2 * PI / edgeCount * i
        yield(vector3D(cos(theta), sin(theta), 0) * interval)
    }
}.map(::deploy).toList()

private fun deploy(p: Vector3D) = p + java.util.Random().run {
    vector3D(nextGaussian(), nextGaussian(), nextGaussian()) * .1
}

fun main() {
    val world = world(beacons.mapIndexed { i, p -> Beacon(i) to p }.toMap())
    val grid = world.edges().map { it.toList().map { (b, _) -> b.id } }
    val system = LocatingSystem(maxMeasure).apply { this[-1L] = world.preMeasures() }

    val remote = remoteHub("实验").apply {
        openAllNetworks()
        println(networksInfo())
        paintFrame3("实际地图", grid.map { it.map(beacons::get) })
        system.painter = { paintFrame3("步骤", it.toPoints()) }
    }

    println("optimize in ${measureTimeMillis { system.optimize() }}ms")

    val mobile = Beacon(beacons.size)
    val steps = 400
    val l = vector3D(1, 1, 0) * interval * cos(PI / edgeCount)
    for (i in 0 until steps) {
        val k = 1 - i.toDouble() / steps
        val theta = 4 * PI * k
        val m = vector3D(cos(theta), sin(theta), 0) * l * k - vector3D(0, 0, 1.5)
        val time = System.currentTimeMillis()
        system[time] = world.measure(mobile.move(time), m).toMap()

        val part = system[mobile].toPoints()
        val result = part.single().last().run { copy(z = -abs(z)) }

        with(remote) {
            paintFrame3("实际地图", grid.map { it.map(beacons::get) })
            paint("目标", m)
            paint("历史", result)
        }

        println("step $i: ${m euclid result}\t${m.select(0..1) euclid result.select(0..1)}")
    }
}

private operator fun LocatingSystem.set(t: Long, map: Map<Pair<Position, Position>, Double>) {
    for ((pair, l) in map) {
        val (a, b) = pair
        this[a, b, t] = l
    }
}

private fun Map<Beacon, Vector3D>.toPoints(): List<List<Vector3D>> {
    val tf = mapNotNull { (key, p) -> beacons.getOrNull(key.id)?.to(p) }.toTransformationWithSVD(1e-8)
    return listOf(map { (_, p) -> (tf * p).to3D() })
}
