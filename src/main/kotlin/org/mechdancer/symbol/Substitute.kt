package org.mechdancer.symbol

import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.linear.ExpressionVector

// 代入

class ValueCalculator internal constructor(e: Expression) {
    var expression = e
        private set

    operator fun set(from: Expression, to: Expression) {
        expression = expression.substitute(from, to)
    }

    operator fun set(from: Expression, to: Number) {
        expression = expression.substitute(from, Constant(to.toDouble()))
    }
}

fun Expression.substitute(e: Expression, n: Number) =
    substitute(e, Constant(n.toDouble()))

fun Expression.substitute(block: ValueCalculator.() -> Unit) =
    ValueCalculator(this).apply(block).expression

fun Expression.substitute(expressionVector: ExpressionVector) =
    expressionVector.expressions.entries.fold(this) { r, (v, e) -> r.substitute(v, e) }
