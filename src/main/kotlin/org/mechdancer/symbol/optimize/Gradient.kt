package org.mechdancer.symbol.optimize

import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.implement.vector.toListVector
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.VariableSpace
import org.mechdancer.symbol.linear.Hamiltonian
import org.mechdancer.symbol.linear.NamedExpressionVector
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
    vararg domains: Domain,
    alpha: (Double) -> Double
): OptimizeStep<NamedExpressionVector> {
    val gradient = Hamiltonian.gradient(error.d(), space).toFunction(space)
    return { p ->
        val v = p.toVector(space)
        val limit = domains.mapNotNull { it.mapExp(p) }
        val g = if (limit.isNotEmpty()) {
            val values = limit.associate { (v, _, d) -> v to d.toDouble() }
            gradient(v) + space.variables.map { values[it] ?: .0 }.toListVector()
        } else {
            gradient(v)
        }
        val l = g.length
        val a = alpha(l)
        p - space.order(g) * a to l * a
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
    vararg domains: Domain
): OptimizeStep<NamedExpressionVector> {
    val gradient = Hamiltonian.gradient(error.d(), space).toFunction(space)
    return { p ->
        val dp = space.order(gradient(p.toVector(space)))
        domains.fastestOf(error, p, dp, Domain::mapLinear)
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
    block: (error: Expression) -> OptimizeStep<NamedExpressionVector>
): OptimizeStep<NamedExpressionVector> {
    val dim = errors.size
    // 样本步骤函数
    val steps = errors.map(block)
    // 迭代函数
    var i = 0
    return { steps[i++ % dim](it) }
}
