package org.mechdancer.symbol.optimize

import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.Sum
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.Hamiltonian
import org.mechdancer.symbol.linear.VariableSpace
import org.mechdancer.symbol.toDouble

/**
 * 标准的完全梯度下降优化
 *
 * @param error 损失函数
 * @param space 变量空间
 * @param alpha 学习率系数
 * @return 优化步骤函数
 */
fun batchGD(
    error: Expression,
    space: VariableSpace,
    alpha: (Double) -> Double
): OptimizeStep<ExpressionVector> {
    val gradient = Hamiltonian.dfToGrad(error.d(), space)
    return { p ->
        val g = gradient.substitute(p)
        val l = g.length().toDouble()
        val a = alpha(l)
        p - g * a to l * a
    }
}

/**
 * 使用牛顿迭代确定步长的最速下降法
 *
 * @param error 损失函数
 * @param space 变量空间
 * @return 优化步骤函数
 */
fun fastestBatchGD(
    error: Expression,
    space: VariableSpace,
    vararg conditions: Domain
): OptimizeStep<ExpressionVector> {
    val gradient = Hamiltonian.dfToGrad(error.d(), space)
    return { p ->
        fastestWithNewton(Sum[conditions.mapNotNull { it.check(p) } + error], p, gradient.substitute(p))
    }
}

/**
 * 采用均方损失函数且大步长优先的随机梯度下降法
 *
 * @param errors 样本损失函数
 * @param block 依赖的批量梯度下降函数
 * @return 优化步骤函数
 */
inline fun stochasticGD(
    errors: List<Expression>,
    block: (error: Expression) -> OptimizeStep<ExpressionVector>
): OptimizeStep<ExpressionVector> {
    val dim = errors.size
    // 样本步骤函数
    val steps = errors.map(block)
    // 迭代函数
    var i = 0
    return { steps[i++ % dim](it) }
}
