package org.mechdancer.symbol.optimize

import org.mechdancer.symbol.`^`
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.Hamiltonian
import org.mechdancer.symbol.linear.VariableSpace
import org.mechdancer.symbol.toDouble
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
 * 基础梯度下降法
 *
 * @param samples 样本函数
 * @param space 变量空间
 * @param alpha 学习率系数
 * @return 优化步骤函数
 */
fun sgd(samples: List<Expression>,
        space: VariableSpace,
        alpha: (Double) -> Double
): OptimizeStep<ExpressionVector> {
    val lastStep = DoubleArray(samples.size) { it.toDouble() - samples.size }
    val steps = samples.map { gradientDescent(it `^` 2, space, alpha) }
    val list = PriorityQueue(steps.size, Comparator<Int> { a, b -> lastStep[a].compareTo(lastStep[b]) })
    list.addAll(steps.indices)
    var i = 0
    return {
        val next = list.poll()
        println("$i\t$next")
        steps[i++ % steps.size](it).also { (_, s) ->
            lastStep[next] = -s
            list.offer(next)
        }
    }
}
