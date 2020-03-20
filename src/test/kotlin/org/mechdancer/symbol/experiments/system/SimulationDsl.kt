package org.mechdancer.symbol.experiments.system

import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.function.vector.select
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.networksInfo
import org.mechdancer.symbol.paint
import org.mechdancer.symbol.paintFrame3
import org.mechdancer.symbol.system.Beacon
import org.mechdancer.symbol.system.LocatingSystem
import org.mechdancer.symbol.system.WorldBuilderDsl
import kotlin.math.abs
import kotlin.system.measureTimeMillis

class SimulationDsl private constructor() {
    var maxMeasure: Double = 30.0
    private var layout: Sequence<Vector3D>? = null
    private var trace: Sequence<Vector3D> = emptySequence()
    private var buildWorld: WorldBuilderDsl.() -> Unit = {}

    fun layout(block: suspend SequenceScope<Vector3D>.() -> Unit) {
        layout = sequence(block)
    }

    fun trace(block: suspend SequenceScope<Vector3D>.() -> Unit) {
        trace = sequence(block)
    }

    fun world(block: WorldBuilderDsl.() -> Unit) {
        buildWorld = block
    }

    companion object {
        fun simulate(block: SimulationDsl.() -> Unit) =
            SimulationDsl().apply(block).run {
                val mobile = Beacon(99)
                val world = layout!!
                    .withIndex()
                    .associate { (i, p) -> Beacon(i) to p }
                    .let { WorldBuilderDsl.world(it, maxMeasure, buildWorld) }
                val system = LocatingSystem(maxMeasure).apply { this[-1L] = world.preMeasures() }

                val remote = remoteHub("实验").apply {
                    openAllNetworks()
                    println(networksInfo())
                    paintFrame3("实际地图", world.grid())
                    system.painter = { paintFrame3("步骤", world.grid(world.transform(it))) }
                }

                println("optimize in ${measureTimeMillis { system.optimize() }}ms")

                for ((i, m) in trace.withIndex()) {
                    val result = System.currentTimeMillis().let { time ->
                        system[time] = world.measure(mobile.move(time), m).toMap()
                        system[mobile].let(world::transform).getValue(mobile).run { copy(z = -abs(z)) }
                    }
                    with(remote) {
                        paintFrame3("实际地图", world.grid())
                        paint("目标", m)
                        paint("历史", result)
                    }
                    println("step $i: ${m euclid result}\t${m.select(0..1) euclid result.select(0..1)}")
                }
            }
    }
}
