package org.mechdancer.symbol

import org.mechdancer.algebra.function.matrix.inverse
import org.mechdancer.algebra.function.matrix.times
import org.mechdancer.algebra.function.vector.dot
import org.mechdancer.algebra.function.vector.normalize
import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.Hamiltonian.Companion.dfToGrad
import org.mechdancer.symbol.linear.HessianMatrix
import org.mechdancer.symbol.linear.VariableSpace

/** 优化步骤函数 := 当前位置 -> (新位置, 实际步长) */
typealias OptimizeStep = (ExpressionVector) -> Pair<ExpressionVector, Double>

fun gradientDescent(
    error: Expression,
    space: VariableSpace,
    pid: PIDLimiter
): OptimizeStep {
    val gradField = space.hamiltonian * error
    return { p ->
        val grad = gradField.substitute(p)
        val l = grad.length().toDouble()
        val k = pid(l)
        p - grad * (k / l) to k
    }
}

/**
 * 牛顿迭代优化
 *
 * @param error 损失函数
 * @param space 变量空间
 * @return 优化步骤函数
 */
fun newton(error: Expression, space: VariableSpace): OptimizeStep {
    val df = error.d()                         // 一阶全微分表达式
    val gradient = dfToGrad(df, space)         // 梯度表达式
    val hessian = HessianMatrix(df.d(), space) // 海森矩阵表达式
    val order = space.variables.toList()       // 向量维度顺序
    return { p ->
        val step = run {
            val g = gradient.toVector(p, space)       // 梯度
            val h = hessian.toMatrix(p).inverse() * g // 海森极值增量
            val k = h.normalize() dot g.normalize()   // 归一化方向系数
            h * k + g * (1 - k)
        }
        order // 向量转化为表达式向量
            .mapIndexed { i, v -> v to Constant(step[i]) }
            .let { p - ExpressionVector(it.toMap()) to step.length }
    }
}

/** 递推计算 */
fun <T> recurrence(
    init: T, block: (T) -> T
) = sequence {
    var t = init
    while (true) {
        t = block(t)
        yield(t)
    }
}

inline fun <T : Any> Sequence<T>.firstOrLast(
    block: (T) -> Boolean
): T {
    var last: T? = null
    for (t in this) {
        if (block(t)) return t
        last = t
    }
    return last ?: throw NoSuchElementException("Sequence is empty.")
}

class PIDLimiter(
    private val ka: Double,
    private val ki: Double,
    private val kd: Double,
    private val max: Double
) {
    private var last = .0
    private var sum = .0

    operator fun invoke(e: Double): Double {
        val dd = e - last
        last = e
        sum = .9 * sum + .1 * e
        val r = ka * (e + ki * sum + kd * dd)
        return when {
            r > +max -> {
                clear()
                +max
            }
            r < -max -> {
                clear()
                -max
            }
            else     -> r
        }
    }

    private fun clear() {
        last = .0
        sum = .0
    }
}
