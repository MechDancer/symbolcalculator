package org.mechdancer.symbol.optimize

import org.mechdancer.symbol.`^`
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Constant.Companion.`-∞`
import org.mechdancer.symbol.core.Constant.Companion.zero
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.FunctionExpression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.div
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.minus

/**
 * 条件收集器
 *
 * 逐条输入方程、不等式和初始条件
 * 产生均方差表达式、变量约束条件和初始值
 */
class ConditionCollector {
    private var i = 0

    private val initValues =
        mutableMapOf<Variable, Constant>()

    private val equations =
        mutableListOf<FunctionExpression>()

    private val domains =
        mutableListOf<Domain>()

    operator fun plusAssign(e: Expression) {
        if (e is FunctionExpression) equations += e
    }

    operator fun set(v: Variable, init: Double) =
        initValues.set(v, Constant(init))

    fun domain(e: Expression) =
        when (e) {
            is Constant -> throw UnsupportedOperationException()
            is Variable -> {
                domains += e[`-∞`, zero]
                e
            }
            else        -> {
                val lambda = Variable("λ${i++}")
                equations += (e - lambda) as FunctionExpression
                domains += lambda[`-∞`, zero]
                lambda
            }
        }

    fun build() =
        Triple(equations.run { map { (it `^` 2) / (2 * size) } },
               domains.toTypedArray(),
               initValues.let(::ExpressionVector))
}
