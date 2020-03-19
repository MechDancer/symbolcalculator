package org.mechdancer.symbol.experiments.system

import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.function.vector.select
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector2D
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.algebra.implement.vector.vector3DOfZero
import org.mechdancer.geometry.angle.toRad
import org.mechdancer.geometry.angle.toVector
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.networksInfo
import org.mechdancer.symbol.paint
import org.mechdancer.symbol.paintFrame3
import org.mechdancer.symbol.system.Beacon
import org.mechdancer.symbol.system.LocatingSystem
import org.mechdancer.symbol.system.WorldBuilderDsl.Companion.world
import kotlin.math.*
import kotlin.system.measureTimeMillis

// 6 个固定标签组成矩形，1 个移动标签沿矩形对角线运动

private const val maxMeasure = 30.0
private const val edgeCount = 6
private val radius = when {
    edgeCount < 6 -> maxMeasure * .95 * .5 / tan(PI / edgeCount)
    else          -> maxMeasure * .95
}

private val beacons = sequence {
    yield(vector3DOfZero())
    for (i in 0 until edgeCount) {
        val theta = 2 * PI / edgeCount * i
        yield(vector3D(cos(theta), sin(theta), 0) * radius)
    }
}.map(::deploy).toList()

private fun deploy(p: Vector3D) = p + java.util.Random().run {
    vector3D(nextGaussian(), nextGaussian(), nextGaussian()) * .1
}

fun main() {
    println("面积/标签数 = ${edgeCount * radius * radius * sin(2 * PI / edgeCount) / 2 / (edgeCount + 1)}")

    val world = world(beacons.mapIndexed { i, p -> Beacon(i) to p }.toMap())
    val grid = world.edges().map { it.toList().map { (b, _) -> b.id } }
    val system = LocatingSystem(maxMeasure).apply { this[-1L] = world.preMeasures() }

    val remote = remoteHub("实验").apply {
        openAllNetworks()
        println(networksInfo())
        paintFrame3("实际地图", grid.map { it.map(beacons::get) })
        system.painter = { paintFrame3("步骤", listOf(world.transform(it))) }
    }

    println("optimize in ${measureTimeMillis { system.optimize() }}ms")

    val mobile = Beacon(beacons.size)
    val steps = 400
    val l = vector2D(1, 1) * radius * cos(PI / edgeCount)
    for (i in 0 until steps) {
        val k = 1 - i.toDouble() / steps
        val (x, y) = (4 * PI * k).toRad().toVector() * l * k
        val m = vector3D(x, y, -1.5)
        val result = System.currentTimeMillis().let {
            system[it] = world.measure(mobile.move(it), m).toMap()
            world.transform(system[mobile]).last().run { copy(z = -abs(z)) }
        }
        with(remote) {
            paintFrame3("实际地图", grid.map { it.map(beacons::get) })
            paint("目标", m)
            paint("历史", result)
        }
        println("step $i: ${m euclid result}\t${m.select(0..1) euclid result.select(0..1)}")
    }
}
