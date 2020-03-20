package org.mechdancer.symbol.experiments.system

import org.mechdancer.algebra.function.vector.div
import org.mechdancer.algebra.function.vector.minus
import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.symbol.experiments.system.SimulationDsl.Companion.simulate
import kotlin.math.sqrt

fun main() = simulate {
    world {
        sigmaMeasure = .1
        sigmaDeploy = vector3D(1, 1, 1) * .2
    }
    maxMeasure = 30.0
    // 6 个固定标签组成矩形，1 个移动标签沿矩形对角线运动
    val lx = .5
    val ly = sqrt(1 - lx * lx)
    val shape = vector3D(lx, ly, 0) * maxMeasure * .95 + vector3D(0, 0, 1)
    layout {
        for (i in 0 until 6) yield(vector3D(i / 2, i % 2, 0) * shape)
    }
    trace {
        val steps = 200
        val dl = vector3D(2, 1, 0) * shape / steps
        for (i in 0 until steps) yield(dl * i - vector3D(0, 0, 1.5))
    }
}
