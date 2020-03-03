package org.mechdancer.symbol.optimize

import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.Hamiltonian
import org.mechdancer.symbol.linear.VariableSpace
import java.util.*

/**
 * 基础梯度下降法
 *
 * @param error 损失函数
 * @param space 变量空间
 * @param alpha 学习率系数
 * @return 优化步骤函数
 */
fun gradientDescent(
    error: Expression,
    space: VariableSpace,
    alpha: (Double) -> Double
): OptimizeStep<ExpressionVector> {
    val gradient = Hamiltonian.dfToGrad(error.d(), space)
    return { p ->
        val g = gradient.substitute(p)
        val l = alpha(g.length().toDouble())
        p - g * l to l
    }
}

/**
 * 使用牛顿迭代确定步长的最速下降法
 *
 * @param error 损失函数
 * @param space 变量空间
 * @return 优化步骤函数
 */
fun gradientDescent(
    error: Expression,
    space: VariableSpace
): OptimizeStep<ExpressionVector> {
    val gradient = Hamiltonian.dfToGrad(error.d(), space)
    val l by variable
    return { p ->
        val g = gradient.substitute(p)
        val lh = ExpressionVector(g.expressions.mapValues { (_, e) -> e * l })
        val fh = newton(error.substitute(p - lh), l)
        val (lo, _) = recurrence(1.0 to .0) { (p, _) -> fh(p) }.take(100).firstOrLast { (_, s) -> s < 1e-9 }
        p - lh.substitute(l, Constant(lo)) to g.length().toDouble() * lo
    }
}

/**
 * 大步优先的随机梯度下降法
 *
 * @param samples 样本函数
 * @param block 依赖的批量梯度下降函数
 * @return 优化步骤函数
 */
inline fun stochasticGD(
    samples: List<Expression>,
    block: (error: Expression) -> OptimizeStep<ExpressionVector>
): OptimizeStep<ExpressionVector> {
    val dim = samples.size
    // 随机损失函数
    val steps = samples.map { block((it `^` 2) / dim) }
    // 样本优化
    val lastStep = DoubleArray(dim) { it.toDouble() - dim }
    val stepQueue = PriorityQueue(dim, Comparator<Int> { a, b -> lastStep[a].compareTo(lastStep[b]) })
        .apply { addAll(steps.indices) }
    // 迭代函数
    var i = 0
    return {
        val next = stepQueue.poll()
        println("$i\t$next")
        steps[i++ % steps.size](it).also { (_, s) ->
            lastStep[next] = -s
            stepQueue.offer(next)
        }
    }
}
