package org.mechdancer.symbol

import org.mechdancer.symbol.linear.Field
import java.util.*
import kotlin.math.abs

private fun pointXYZ(x: Number, y: Number, z: Number) =
    Field(mapOf(Variable("x") to Constant(x.toDouble()),
                Variable("y") to Constant(y.toDouble()),
                Variable("z") to Constant(z.toDouble())))

// 地图
private val beacons =
    listOf(pointXYZ(0, 0, 0),
           pointXYZ(30, 0, 0),
           pointXYZ(0, 30, 0),
           pointXYZ(30, 30, 0)
    ).let { it + it + it + it }
// 移动标签
private val mobile =
    pointXYZ(15, 20, -3)

// 测量函数

private val engine = Random()
fun measure(b: Field, m: Field) =
    (b - m).length as Constant * (1 + 5E-3 * engine.nextGaussian()) + 1E-2 * engine.nextGaussian()

fun main() {
    // 设定变量空间
    val space by variableSpace("x", "y", "z")
    // 确定误差函数固定结构
    val struct = beacons.map { (space.ordinaryField - it).length }.withIndex()
    repeat(100) {
        // 绑定测距值
        val map = beacons.map { measure(it, mobile) }
        val t0 = System.currentTimeMillis()
        // 求误差函数
        val error = struct.sumBy { (i, e) -> e - map[i] `^` 2 }
        // 构造梯度下降迭代函数
        // 无误差不重复测最优参数：PIDLimiter(.4, .02, .02, 30.0)
        val f = gradientDescent(error, space, PIDLimiter(.01, .02, .02, 30.0))
        val t1 = System.currentTimeMillis()
        // 设定初始值
        val result =
            recurrence(pointXYZ(1, 0, -1) to Double.MAX_VALUE) { (p, _) -> f(p) }
                .take(200)                                   // 设定最大迭代次数
                .firstOrNull { (_, step) -> abs(step) < 5E-3 } // 设定收敛判定条件
                ?.first                                        // 取出结果
        val t2 = System.currentTimeMillis()
        println("总耗时 = 求梯度 + 迭代 = ${t1 - t0}ms + ${t2 - t1}ms = ${t2 - t0}ms")
        println("误差 = ${(result!! - mobile).length}")
        println()
    }
}
