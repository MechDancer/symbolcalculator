package org.mechdancer.symbol.linear

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.implement.vector.toListVector
import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.Variable

/** 表达式向量 */
inline class ExpressionVector(val expressions: Map<Variable, Expression>) {
    val dim get() = expressions.size
    operator fun get(v: Variable) = expressions[v]
    override fun toString() = expressions.entries.joinToString("\n") { (v, e) -> "$v -> $e" }

    private fun zip(others: ExpressionVector, block: (Expression, Expression) -> Expression): ExpressionVector {
        require(expressions.keys == others.expressions.keys)
        return ExpressionVector(expressions.mapValues { (v, e) -> block(e, others.expressions.getValue(v)) })
    }

    operator fun plus(others: ExpressionVector) = zip(others, Expression::plus)
    operator fun minus(others: ExpressionVector) = zip(others, Expression::minus)
    operator fun times(k: Double) = map { it * k }
    operator fun div(k: Double) = map { it / k }

    fun length() = expressions.values.sumBy { it `^` 2 }.let(::sqrt)

    inline fun map(block: (Expression) -> Expression) =
        expressions.mapValues { (_, e) -> block(e) }.let(::ExpressionVector)

    fun substitute(from: Expression, to: Expression) =
        map { it.substitute(from, to) }

    fun substitute(others: ExpressionVector) =
        others.expressions.entries.fold(expressions) { r, (v, e) ->
            r.mapValues { (_, e0) -> e0.substitute(v, e) }
        }.let(::ExpressionVector)

    fun toVector(values: ExpressionVector, order: VariableSpace): Vector {
        val valueSave = substitute(values)
        return order.variables.map { valueSave[it]!!.toDouble() }.toListVector()
    }
}
