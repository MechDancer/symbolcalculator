package org.mechdancer.symbol

import org.mechdancer.symbol.linear.Field

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

fun Expression.substitute(field: Field) =
    field.expressions.entries.fold(this) { r, (v, e) -> r.substitute(v, e) }

fun Field.substitute(field: Field) =
    field.expressions.entries.fold(expressions) { r, (v, e) ->
        r.mapValues { (_, e0) -> e0.substitute(v, e) }
    }.let(::Field)
