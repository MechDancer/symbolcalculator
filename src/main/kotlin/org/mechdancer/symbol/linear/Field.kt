package org.mechdancer.symbol.linear

import org.mechdancer.symbol.*

class Field(internal val expressions: Map<Variable, Expression>) {
    val dim get() = expressions.size
    operator fun get(v: Variable) = expressions[v]
    override fun toString() =
        expressions
            .entries
            .joinToString("\n") { (v, e) -> "$v -> $e" }

    val length by lazy {
        expressions.entries.sumBy { (_, e) -> Power[e, Constant(2.0)] }.let(::sqrt)
    }

    private fun zip(others: Field, block: (Expression, Expression) -> Expression): Field {
        require(expressions.keys == others.expressions.keys)
        return Field(expressions.mapValues { (v, e) -> block(e, others.expressions.getValue(v)) })
    }

    operator fun plus(others: Field) = zip(others, Expression::plus)
    operator fun minus(others: Field) = zip(others, Expression::minus)
    operator fun times(k: Double) =
        Field(expressions.mapValues { (_, e) -> e * k })

    operator fun div(k: Double) =
        Field(expressions.mapValues { (_, e) -> e / k })
}
