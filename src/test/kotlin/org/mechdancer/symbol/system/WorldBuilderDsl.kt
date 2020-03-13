package org.mechdancer.symbol.system

import org.mechdancer.algebra.implement.vector.Vector3D

class WorldBuilderDsl private constructor() {
    var temperature = 15.0
    var actualTemperature = 15.0
    var maxMeasureTime: Long? = null
    var sigmaMeasure = .01

    private fun defaultMeasureTime() =
        (30_000 / SimulationWorld.soundVelocity(actualTemperature)).toLong()

    companion object {
        fun world(beacons: Map<Beacon, Vector3D>,
                  block: WorldBuilderDsl.() -> Unit = {}
        ) =
            WorldBuilderDsl()
                .apply(block)
                .run {
                    SimulationWorld(beacons = beacons,
                                    temperature = temperature,
                                    actualTemperature = actualTemperature,
                                    maxMeasureTime = maxMeasureTime ?: defaultMeasureTime(),
                                    sigmaMeasure = sigmaMeasure)
                }
    }
}
