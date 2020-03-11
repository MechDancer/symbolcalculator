package org.mechdancer.symbol.optimize

import org.mechdancer.symbol.`^`
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Constant.Companion.`-∞`
import org.mechdancer.symbol.core.Constant.Companion.zero
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.FunctionExpression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.div
import org.mechdancer.symbol.minus

class ConditionCollector {
    private val values =
        mutableListOf<Double?>()

    private val equations =
        mutableListOf<FunctionExpression>()

    private val domains =
        mutableListOf<Domain>()

    operator fun plusAssign(e: Expression) {
        if (e is FunctionExpression) equations += e
    }

    fun domain(e: Expression, init: Double?) {
        when (e) {
            is Constant -> Unit
            is Variable -> domains += e[`-∞`, zero]
            else        -> {
                values += init
                val lambda = Variable("λ${values.lastIndex}")
                equations += (e - lambda) as FunctionExpression
                domains += lambda[`-∞`, zero]
            }
        }
    }

    fun build() =
        Triple(equations.run { map { (it `^` 2) / (2 * size) } },
               domains.toTypedArray(),
               values.withIndex().associate { (i, value) ->
                   Variable("λ$i") to value
               })
}
