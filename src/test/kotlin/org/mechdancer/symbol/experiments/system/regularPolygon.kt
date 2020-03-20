package org.mechdancer.symbol.experiments.system

import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.vector2D
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.algebra.implement.vector.vector3DOfZero
import org.mechdancer.geometry.angle.toRad
import org.mechdancer.geometry.angle.toVector
import org.mechdancer.symbol.experiments.system.SimulationDsl.Companion.simulate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

fun main() = simulate {
    maxMeasure = 30.0
    world {
        sigmaMeasure = .01
        sigmaDeploy = vector3D(1, 1, 1) * .2
    }
    val edgeCount = 6
    val radius = when {
        edgeCount < 6 -> maxMeasure * .95 * .5 / tan(PI / edgeCount)
        else          -> maxMeasure * .95
    }
    println("面积/标签数 = ${edgeCount * radius * radius * sin(2 * PI / edgeCount) / 2 / (edgeCount + 1)}")
    layout {
        yield(vector3DOfZero())
        for (i in 0 until edgeCount) {
            val theta = 2 * PI / edgeCount * i
            yield(vector3D(cos(theta), sin(theta), 0) * radius)
        }
    }
    trace {
        val steps = 400
        val l = vector2D(1, 1) * radius * cos(PI / edgeCount)
        for (i in 0 until steps) {
            val k = 1 - i.toDouble() / steps
            val (x, y) = (4 * PI * k).toRad().toVector() * l * k
            yield(vector3D(x, y, -1.5))
        }
    }
}
