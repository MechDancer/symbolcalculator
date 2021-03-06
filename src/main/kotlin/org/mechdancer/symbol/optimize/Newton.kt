package org.mechdancer.symbol.optimize

import org.mechdancer.algebra.function.matrix.inverse
import org.mechdancer.algebra.function.matrix.times
import org.mechdancer.algebra.function.vector.dot
import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.VariableSpace
import org.mechdancer.symbol.linear.Hamiltonian.Companion.gradient
import org.mechdancer.symbol.linear.Hessian.Companion.hessian
import org.mechdancer.symbol.linear.NamedExpressionVector

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
): OptimizeStep<NamedExpressionVector> {
    val df = error.d()
    val gradient = gradient(df, space).toFunction(space)
    val hessian = hessian(df.d(), space).toFunction(space)
    return { p ->
        val v = p.toVector(space)
        val g = gradient(v)
        val h = hessian(v).inverse() * g
        val s = h dot g
        val step = if (s < 0) g else {
            val k = s / h.length / g.length
            h * k + g * (1 - k)
        }
        p - space.order(step) to step.length
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
    vararg domains: Domain
): OptimizeStep<NamedExpressionVector> {
    // 微分
    val df = error.d()
    val gradient = gradient(df, space).toFunction(space)
    val hessian = hessian(df.d(), space).toFunction(space)
    return { p ->
        val v = p.toVector(space)
        val g = gradient(v)
        val h = hessian(v).inverse() * g
        // 确定最优下降方向
        val dp = if (g dot h < 0) g else h
        domains.fastestOf(error, p, space.order(dp), Domain::mapLinear)
    }
}
