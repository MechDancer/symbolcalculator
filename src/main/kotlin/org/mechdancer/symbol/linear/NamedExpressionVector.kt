package org.mechdancer.symbol.linear

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.implement.vector.toListVector
import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.core.VariableSpace
import kotlin.streams.toList

/** 表达式向量 */
inline class NamedExpressionVector(val expressions: Map<Variable, Expression>) {
    val dim get() = expressions.size
    operator fun get(v: Variable) = expressions[v]
    operator fun get(v: Iterable<Variable>) = v.map(expressions::get)
    override fun toString() = expressions.entries.joinToString("\n") { (v, e) -> "$v -> $e" }

    private fun zip(others: NamedExpressionVector, block: (Expression, Expression) -> Expression) =
        NamedExpressionVector(expressions.mapValues { (v, e) -> others.expressions[v]?.let { block(e, it) } ?: e })

    operator fun plus(others: NamedExpressionVector) = zip(others, Expression::plus)
    operator fun minus(others: NamedExpressionVector) = zip(others, Expression::minus)
    operator fun times(k: Double) = map { it * k }
    operator fun div(k: Double) = map { it / k }

    fun length() = expressions.values.sumBy { it `^` 2 }.let(::sqrt)

    inline fun map(block: (Expression) -> Expression) =
        expressions.mapValues { (_, e) -> block(e) }.let(::NamedExpressionVector)

    fun substitute(from: Expression, to: Expression) =
        map { it.substitute(from, to) }

    fun substitute(others: NamedExpressionVector) =
        if (dim >= 6)
            others.expressions.entries.fold(expressions.values) { r, (v, e) ->
                r.parallelStream().map { e0 -> e0.substitute(v, e) }.toList()
            }.let { NamedExpressionVector(expressions.keys.zip(it).toMap()) }
        else
            others.expressions.entries.fold(expressions) { r, (v, e) ->
                r.mapValues { (_, e0) -> e0.substitute(v, e) }
            }.let(::NamedExpressionVector)

    fun toVector(space: VariableSpace): Vector =
        space.variables.map { expressions[it]?.toDouble() ?: .0 }.toListVector()
}
