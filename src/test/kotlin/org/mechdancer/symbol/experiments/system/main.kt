package org.mechdancer.symbol.experiments.system

import org.mechdancer.algebra.implement.vector.to3D
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.geometry.transformation.toTransformationWithSVD
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.networksInfo
import org.mechdancer.symbol.paintFrame3

fun main() {
    val a = listOf(
        vector3D(0, 0, 0),
        vector3D(1, 0, 0),
        vector3D(0, 1, 0),
        vector3D(0, 0, 1))
    val b = listOf(
        vector3D(0, 0, 0),
        vector3D(1, 0, 0),
        vector3D(0, 1, 0),
        vector3D(0, 0, -1))
    val tf = a.zip(b).toTransformationWithSVD(1e-8)
    val c = b.map { (tf * it).to3D() }

    remoteHub("实验").apply {
        openAllNetworks()
        println(networksInfo())
        while (true) {
            paintFrame3("a", listOf(a))
            paintFrame3("b", listOf(b))
            paintFrame3("c", listOf(c))
        }
    }
}
