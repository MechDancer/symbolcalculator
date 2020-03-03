package org.mechdancer.symbol.optimize

import org.mechdancer.algebra.function.matrix.inverse
import org.mechdancer.algebra.function.matrix.times
import org.mechdancer.algebra.function.vector.dot
import org.mechdancer.algebra.function.vector.normalize
import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.listVectorOfZero
import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.Hamiltonian.Companion.dfToGrad
import org.mechdancer.symbol.linear.HessianMatrix
import org.mechdancer.symbol.linear.VariableSpace
import kotlin.math.abs
import kotlin.math.sign

/** 优化步骤函数 := 当前位置 -> (新位置, 实际步长) */
typealias OptimizeStep<T> = (T) -> Pair<T, Double>

/**
 * 基础多元牛顿迭代法
 *
 * @param error 损失函数
 * @param space 变量空间
 * @return 优化步骤函数
 */
fun newton(
    error: Expression,
    space: VariableSpace
): OptimizeStep<ExpressionVector> {
    val df = error.d()                         // 一阶全微分表达式
    val gradient = dfToGrad(df, space)         // 梯度表达式
    val hessian = HessianMatrix(df.d(), space) // 海森矩阵表达式
    return { p ->
        val g = gradient.toVector(p, space)       // 梯度
        val h = hessian.toMatrix(p).inverse() * g // 海森极值增量
        val s = h dot g                           // 方向系数
        val step = if (s < 0) g else {
            val k = s / h.length / g.length
            h * k + g * (1 - k)
        }
        p - space.order(step) to step.length
    }
}

/**
 * 基本一元牛顿迭代优化
 *
 * @param error 损失函数
 * @param v 变量
 * @return 优化步骤函数
 */
fun newton(
    error: Expression,
    v: Variable
): OptimizeStep<Double> {
    val df = error.d() / v.d()
    val ddf = df.d() / v.d()
    return { p ->
        val g = df.substitute(v, Constant(p)).toDouble()
        val h = g / ddf.substitute(v, Constant(p)).toDouble()
        val step = if (h.sign != g.sign) g else h
        p - step to abs(step)
    }
}

/**
 * 阻尼牛顿迭代优化
 *
 * @param error 损失函数
 * @param space 变量空间
 * @return 优化步骤函数
 */
fun dampingNewton(
    error: Expression,
    space: VariableSpace,
    kM: Double = .5
): OptimizeStep<ExpressionVector> {
    val df = error.d()                         // 一阶全微分表达式
    val gradient = dfToGrad(df, space)         // 梯度表达式
    val hessian = HessianMatrix(df.d(), space) // 海森矩阵表达式
    val l by variable                          // 步长变量

    val zero = listVectorOfZero(space.dim)
    var m = zero
    return { p ->
        val g = gradient.toVector(p, space)       // 梯度
        val h = hessian.toMatrix(p).inverse() * g // 海森极值增量
        // 确定最优下降方向
        val inc = if (g dot h < 0) {
            m = m * (kM - kM * (m.normalize() dot g.normalize())) + g
            m
        } else {
            m = zero
            h
        }
        val dir = inc.normalize()
        // 确定最优下降率
        val lh = space.order(dir).expressions.mapValues { (_, e) -> e * l }.let(::ExpressionVector)
        val fh = newton(error.substitute(p - lh), l)
        val init = inc.length
        val lo = recurrence(init to .0) { (p, _) -> fh(p) }
            .take(100)
            .firstOrLast { (_, s) -> s < init * 1e-9 }.first
        p - space.order(dir * lo) to lo
    }
}
