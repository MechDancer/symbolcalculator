package org.mechdancer.symbol.linear

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.implement.vector.toListVector
import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.Power
import org.mechdancer.symbol.core.Variable
import kotlin.streams.asSequence

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
    operator fun times(k: Double) = ExpressionVector(expressions.mapValues { (_, e) -> e * k })
    operator fun div(k: Double) = ExpressionVector(expressions.mapValues { (_, e) -> e / k })

    fun length() = expressions.entries.sumBy { (_, e) -> Power[e, Constant(2.0)] }.let(::sqrt)

    inline fun map(block: (Expression) -> Expression) =
        expressions.mapValues { (_, e) -> block(e) }.let(::ExpressionVector)

    fun substitute(from: Expression, to: Expression) =
        map { it.substitute(from, to) }

    fun substitute(others: ExpressionVector) =
        others.expressions.entries.fold(expressions) { r, (v, e) ->
            r.mapValues { (_, e0) -> e0.substitute(v, e) }
        }.let(::ExpressionVector)

    fun toVector(values: ExpressionVector, order: VariableSpace): Vector {
        val valueSave = values.expressions.entries
            .fold(expressions) { r, (v, e) ->
                r.entries.parallelStream()
                    .map { (key, e0) -> key to e0.substitute(v, e) }
                    .asSequence()
                    .toMap()
            }
        return order.variables.toList().map { valueSave.getValue(it).toDouble() }.toListVector()
    }
}
