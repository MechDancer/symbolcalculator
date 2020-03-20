package org.mechdancer.symbol.system

import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector3DOfZero

class WorldBuilderDsl private constructor() {
    /** 标签认为的温度（计算温度） */
    var temperature = 15.0

    /** 环境温度（实际温度） */
    var actualTemperature = 15.0

    /** 测距标准差 */
    var sigmaMeasure = .0

    /** 标签部署位置标准差 */
    var sigmaDeploy = vector3DOfZero()

    companion object {
        private val random = java.util.Random()

        fun world(beacons: Map<Beacon, Vector3D>,
                  maxMeasure: Double,
                  block: WorldBuilderDsl.() -> Unit = {}
        ) =
            WorldBuilderDsl()
                .apply(block)
                .run {
                    fun deploy(p: Vector3D) =
                        Vector3D(random.nextGaussian(),
                                 random.nextGaussian(),
                                 random.nextGaussian()
                        ) * sigmaDeploy + p

                    SimulationWorld(
                        layout = beacons.mapValues { (_, p) -> deploy(p) },
                        temperature = temperature,
                        actualTemperature = actualTemperature,
                        maxMeasureTime = (maxMeasure * 1000 / SimulationWorld.soundVelocity(actualTemperature)).toLong(),
                        sigmaMeasure = sigmaMeasure)
                }
    }
}
