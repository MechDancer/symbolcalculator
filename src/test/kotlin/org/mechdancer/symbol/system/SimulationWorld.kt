package org.mechdancer.symbol.system

import org.mechdancer.algebra.function.vector.*
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.to3D
import org.mechdancer.geometry.transformation.toTransformationWithSVD
import org.mechdancer.symbol.*
import org.mechdancer.symbol.optimize.dampingNewton
import org.mechdancer.symbol.optimize.optimize
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * 测距仿真
 *
 * @param layout 固定标签部署结构
 * @param temperature 气温
 * @param thermometer 温度计
 * @param maxMeasureTime 最大测量时间
 * @param sigmaMeasure 测距误差标准差
 */
class SimulationWorld internal constructor(
    private val layout: Map<Beacon, Vector3D>,
    var temperature: Double,
    private val thermometer: (Double) -> Double,
    private val maxMeasureTime: Long,
    private val sigmaMeasure: Double
) {
    // 缓存固定标签之间测距
    private var edges =
        mapOf<Pair<Beacon, Beacon>, Double>()

    /** 预测量（固定标签之间测距） */
    fun preMeasures(): Map<Pair<Position, Position>, Double> {
        // 更新固定标签之间声波飞行时间
        edges = layout.toList().buildEdges(temperature, maxMeasureTime)

        val c0 = soundVelocity(thermometer(temperature))
        return edges.entries.associate { (pair, t) ->
            val (a, b) = pair
            a.static() to b.static() to t * c0 + gaussian(sigmaMeasure)
        }
    }

    /** 位于 [p] 处的移动标签 [mobile] 发起一次测量 */
    fun measure(mobile: Position, p: Vector3D) =
        sequence {
            val c0 = soundVelocity(thermometer(temperature))
            val ca = soundVelocity(temperature)
            for ((beacon, p0) in layout.entries.map { (b, p) -> b.static() to p }) {
                val t = (p euclid p0) / ca
                if (t < maxMeasureTime / 1000.0)
                    yield(beacon to mobile to t * c0 + gaussian(sigmaMeasure))
            }
        }

    /** 固定标签结构（用于画图） */
    fun grid() = edges.keys.map { (a, b) ->
        listOf(layout.getValue(a), layout.getValue(b))
    }

    /** 将一些标签计算位置覆盖到固定标签结构 */
    fun grid(map: Map<Beacon, Vector3D>) =
        sequence {
            val groups = map.keys.groupBy { it in layout }
            val beacons = groups.getValue(true)
            val mobiles = groups[false] ?: emptyList<Beacon>()
            for ((a, b) in edges.keys)
                if (a in beacons && b in beacons)
                    yield(listOf(map.getValue(a), map.getValue(b)))
            for (mobile in mobiles) for (beacon in beacons) {
                val pm = map.getValue(mobile)
                yield(listOf(pm, map.getValue(beacon)))
            }
        }.toList()

    fun transform(
        map: Map<Beacon, Vector3D>,
        actual: Map<Beacon, Vector3D>
    ): Map<Beacon, Vector3D> {
        val pairs0 = map.mapNotNull { (b, p) -> layout[b]?.to(p) }
        val pairs1 = map.mapNotNull { (b, p) -> actual[b]?.to(p) }
        val det = (pairs0 + pairs1).toTransformationWithSVD(1e-8).matrix.det!!
        val (a, b, c) = pairs0
        val (a1, a0) = a
        val (b1, b0) = b
        val (c1, c0) = c
        val d0 = (b0 - a0 cross c0 - a0) + a0
        val d1 = (b1 - a1 cross c1 - a1) * det.sign + a1
        val tf = (pairs0 + (d1 to d0)).toTransformationWithSVD(1e-8)
        return map.mapValues { (_, p) -> (tf * p).to3D() }
    }

    companion object {
        private val random = Random()
        private fun gaussian(sigma: Double = 1.0) = random.nextGaussian() * sigma

        /** [t]℃ 时的声速 */
        fun soundVelocity(t: Double) = 20.048 * sqrt(t + 273.15)

        private val collector = variableCollector()
        private val t by variable(collector) // 飞行时间
        private val x by variable(collector) // 有效声音方向
        private val y by variable(collector) //
        private val z by variable(collector) //
        private val space = collector.toSpace()

        /** 传声模型 := 均匀风场 [wind] 中，声音按声速 [c] 通过向量 [s] 的时间 */
        fun flightDuration(s: Vector3D, wind: Vector3D, c: Double): Double {
            val t0 = (s.length / c).takeIf { it > 0 } ?: return .0
            if (wind.isZero()) return t0

            val error = listOf(
                (x + wind.x) * t - s.x,
                (y + wind.y) * t - s.y,
                (z + wind.z) * t - s.z,
                sqrt(x * x + y * y + z * z) - c
            ).meanSquare()

            val (x0, y0, z0) = s / t0
            val init = point(t to t0, x to x0, y to y0, z to z0)
            val f = dampingNewton(error, space)
            return optimize(init, 200, 1e-9, f)[t]!!.toDouble()
        }

        private fun List<Pair<Beacon, Vector3D>>.buildEdges(
            t: Double,
            maxTime: Long
        ) =
            sequence {
                val ca = soundVelocity(t)
                for (i in indices) for (j in i + 1 until size) {
                    val (a, pa) = get(i)
                    val (b, pb) = get(j)
                    val time = (pa euclid pb) / ca
                    if (time < maxTime / 1000.0) yield(a to b to time)
                }
            }.toMap()

        @JvmStatic
        fun main(args: Array<String>) {
            println(soundVelocity(15.0) / soundVelocity(20.0))
            println(soundVelocity(15.0) / soundVelocity(10.0))
        }

//        @JvmStatic
//        private fun main(args: Array<String>) {
//            val remote = remoteHub("定位优化").apply {
//                openAllNetworks()
//                println(networksInfo())
//            }
//
//            while (true) {
//                fun random() = Random.nextDouble(-1.0, 1.0)
//                val s = vector3D(25, 0, 0)
//                val wind = vector3D(random(), random(), random()).normalize() * (10 + 10 * random())
//                val c = 340.0
//                val t0 = flightDuration(s, vector3DOfZero(), c)
//                val t = flightDuration(s, wind, c)
//
//                remote.paint("关系", wind.length, (t - t0) * c)
//            }
//        }
    }
}
