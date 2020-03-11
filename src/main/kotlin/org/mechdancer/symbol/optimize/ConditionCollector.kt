package org.mechdancer.symbol.optimize

import org.mechdancer.symbol.`^`
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Constant.Companion.`-∞`
import org.mechdancer.symbol.core.Constant.Companion.zero
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.FunctionExpression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.div
import org.mechdancer.symbol.linear.VariableSpace
import org.mechdancer.symbol.minus

class ConditionCollector {
    private var i = 0

    private val equations =
        mutableListOf<FunctionExpression>()

    private val domains =
        mutableListOf<Domain>()

    operator fun plusAssign(e: Expression) {
        if (e is FunctionExpression) equations += e
    }

    fun domain(e: Expression) {
        when (e) {
            is Constant -> Unit
            is Variable -> domains += e[`-∞`, zero]
            else        -> {
                val lambda = Variable("λ${i++}")
                equations += (e - lambda) as FunctionExpression
                domains += lambda[`-∞`, zero]
            }
        }
    }

    fun build() =
        Triple(equations.run { map { (it `^` 2) / (2 * size) } },
               domains.toList(),
               (0 until i).map { Variable("λ$it") }.toSet().let(::VariableSpace))
}
