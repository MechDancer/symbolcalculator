package org.mechdancer.symbol.optimize

import org.mechdancer.algebra.function.matrix.inverse
import org.mechdancer.algebra.function.matrix.times
import org.mechdancer.algebra.function.vector.dot
import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.Hamiltonian
import org.mechdancer.symbol.linear.HessianMatrix
import org.mechdancer.symbol.linear.VariableSpace

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
    val df = error.d()                              // 一阶全微分表达式
    val gradient = Hamiltonian.dfToGrad(df, space) // 梯度表达式
    val hessian = HessianMatrix(df.d(), space)      // 海森矩阵表达式
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
): OptimizeStep<ExpressionVector> {
    // 微分
    val df = error.d()
    val gradient = Hamiltonian.dfToGrad(df, space)
    val hessian = HessianMatrix(df.d(), space)
    return { p ->
        val g = gradient.toVector(p, space)       // 梯度
        val h = hessian.toMatrix(p).inverse() * g // 海森极值增量
        // 确定最优下降方向
        val dp = if (g dot h < 0) g else h
        domains.fastestOf(error, p, space.order(dp), Domain::mapExp)
    }
}
