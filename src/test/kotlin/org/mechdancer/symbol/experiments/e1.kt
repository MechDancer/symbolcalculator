@file:Suppress("ObjectPropertyName", "NonAsciiCharacters")

package org.mechdancer.symbol.experiments

import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.optimize.dampingNewton
import java.util.*
import kotlin.math.abs

private fun pointXYZ(x: Number, y: Number, z: Number) =
    ExpressionVector(mapOf(Variable("x") to Constant(x.toDouble()),
                           Variable("y") to Constant(y.toDouble()),
                           Variable("z") to Constant(z.toDouble())))

private const val 测距次数 = 100

// 地图
private val BEACONS: List<ExpressionVector> =
    listOf(pointXYZ(0, 0, 0),
           pointXYZ(30, 0, 0),
           pointXYZ(0, 30, 0),
           pointXYZ(30, 30, 0)
    ).let {
        val sum = mutableListOf<ExpressionVector>()
        for (i in 1..测距次数) sum += it
        sum
    }

// 移动标签
private val mobile =
    pointXYZ(15, 20, -3)

// 测量函数

private val engine = Random()
private fun measure(b: ExpressionVector, m: ExpressionVector) =
    (b - m).length() * (1 + 1E-3 * engine.nextGaussian()) + 5E-3 * engine.nextGaussian()

fun main() {
    val remote = remoteHub("定位优化").apply {
        openAllNetworks()
        println(networksInfo())
    }
    // 设定变量空间
    val space by variableSpace("x", "y", "z")
    val resultForm = space.ordinaryField
    // 确定误差函数固定结构
    val struct = BEACONS.map { (resultForm - it).length() }.withIndex()
    while (true) {
        // 绑定测距值
        val map = BEACONS.map { measure(it, mobile) }
        val t0 = System.currentTimeMillis()
        // 求损失函数
        val error = struct.sumBy { (i, e) -> (e - map[i]) `^` 2 } / BEACONS.size
        // 构造优化迭代函数
        val f = dampingNewton(error, space)
        val t1 = System.currentTimeMillis()
        // 设定初始值
        val result =
            recurrence(pointXYZ(1, 0, -1) to .0) { (p, _) -> f(p) }
                .take(2000)                            // 设定最大迭代次数
                .first { (_, step) -> abs(step) < 1E-3 } // 设定收敛判定条件
                .first                                   // 取出结果
        val t2 = System.currentTimeMillis()
        remote.paint(result - mobile)
        println("总耗时 = 求梯度 + 迭代 = ${t1 - t0}ms + ${t2 - t1}ms = ${t2 - t0}ms")
    }
}
