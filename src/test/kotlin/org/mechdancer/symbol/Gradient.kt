package org.mechdancer.symbol

import org.mechdancer.symbol.linear.Field
import java.util.*
import kotlin.math.abs

// 地图
private val beacons =
    listOf(point("x" to 0, "y" to 0, "z" to 0),
           point("x" to 30, "y" to 0, "z" to 0),
           point("x" to 0, "y" to 30, "z" to 0),
           point("x" to 30, "y" to 30, "z" to 0))
// 移动标签
private val mobile =
    point("x" to 15, "y" to 20, "z" to -3)

// 测量函数

private val engine = Random()
fun measure(b: Field, m: Field) =
    (b - m).length as Constant * (1 + 5E-3 * engine.nextGaussian()) + 1E-2 * engine.nextGaussian()

fun main() {
    // 设定变量空间
    val space by variableSpace("x", "y", "z")
    // 绑定测距值
    val map = beacons.map { it to measure(it, mobile) }
    // 求误差函数
    val t0 = System.currentTimeMillis()
    val error = map.sumBy { (beacon, measure) -> (space.ordinaryField - beacon).length - measure `^` 2 }
    println("求梯度耗时 = ${System.currentTimeMillis() - t0}ms")
    // 构造梯度下降迭代器
    gradientDescent(error, space, PIDLimiter(.4, .02, .02, 30.0))
        // 设定初始值
        .scan(point("x" to 1, "y" to 0, "z" to -1) to Double.MAX_VALUE)
        // 调用迭代函数
        { (p, _), f -> f(p) }
        // 设定最大迭代次数
        .take(200)
        // 设定收敛判定条件
        .firstOrNull { (_, step) -> abs(step) < 5E-3 }
        // 打印结果
        ?.first
        .let(::println)
}
