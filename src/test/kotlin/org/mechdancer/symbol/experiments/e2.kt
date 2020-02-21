@file:Suppress("ObjectPropertyName", "NonAsciiCharacters")

package org.mechdancer.symbol.experiments

import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.*
import org.mechdancer.symbol.linear.Field
import java.util.*
import kotlin.math.abs

private fun pointXYZ(x: Number, y: Number, z: Number) =
    Field(mapOf(Variable("x") to Constant(x.toDouble()),
                Variable("y") to Constant(y.toDouble()),
                Variable("z") to Constant(z.toDouble())))

private const val 每边点数 = 4

// 地图
private val beacons: List<Field> = run {
    val n = 每边点数 - 1
    val interval = 30.0 / n
    val sum = mutableListOf<Field>()
    for (i in 0 until n) {
        val t = i * interval
        sum += pointXYZ(t, 0, 0)
        sum += pointXYZ(30, t, 0)
        sum += pointXYZ(30 - t, 30, 0)
        sum += pointXYZ(0, 30 - t, 0)
    }
    sum
}

// 移动标签
private val mobile =
    pointXYZ(15, 20, -3)

// 测量函数

private val engine = Random()
private fun measure(b: Field, m: Field) =
    (b - m).length as Constant * (1 + 1E-3 * engine.nextGaussian()) + 5E-3 * engine.nextGaussian()

fun main() {
    val remote = remoteHub("梯度下降").apply {
        openAllNetworks()
        println(networksInfo())
    }
    // 设定变量空间
    val space by variableSpace("x", "y", "z")
    // 确定误差函数固定结构
    val struct = beacons.map { (space.ordinaryField - it).length }.withIndex()
    while (true) {
        // 绑定测距值
        val map = beacons.map { measure(it, mobile) }
        val t0 = System.currentTimeMillis()
        // 求损失函数
        val error = struct.sumBy { (i, e) -> (e - map[i]) `^` 2 } / beacons.size
        // 构造梯度下降迭代函数
        val f = gradientDescent(error, space, PIDLimiter(.1, 2.0, 3.0, 30.0))
        val t1 = System.currentTimeMillis()
        // 设定初始值
        val result =
            recurrence(pointXYZ(1, 0, -1) to .0) { (p, _) -> f(p) }
                .take(200)                            // 设定最大迭代次数
                .first { (_, step) -> abs(step) < 1E-3 } // 设定收敛判定条件
                .first                                   // 取出结果
        val t2 = System.currentTimeMillis()
        remote.paint(result - mobile)
        println("总耗时 = 求梯度 + 迭代 = ${t1 - t0}ms + ${t2 - t1}ms = ${t2 - t0}ms")
    }
}
