package org.mechdancer.symbol.system

import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector3DOfZero

class WorldBuilderDsl private constructor() {
    /** 初始环境气温 */
    var temperature = 15.0

    /** 测距标准差 */
    var sigmaMeasure = .0

    /** 标签部署位置标准差 */
    var sigmaDeploy = vector3DOfZero()

    private var thermometer = { t: Double -> t }

    /** 温度计算法 */
    fun thermometer(block: (Double) -> Double) {
        thermometer = block
    }

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
                        thermometer = thermometer,
                        maxMeasureTime = (maxMeasure * 1000 / SimulationWorld.soundVelocity(temperature)).toLong(),
                        sigmaMeasure = sigmaMeasure)
                }
    }
}
