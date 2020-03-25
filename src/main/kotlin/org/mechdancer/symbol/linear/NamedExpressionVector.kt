package org.mechdancer.symbol.linear

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.implement.vector.toListVector
import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.core.VariableSpace
import org.mechdancer.symbol.core.parallelism

/** 表达式向量 */
inline class NamedExpressionVector(val expressions: Map<Variable, Expression>) {
    val dim get() = expressions.size
    operator fun get(v: Variable) = expressions[v]
    operator fun get(v: Iterable<Variable>) = v.map(expressions::get)
    override fun toString() = expressions.entries.joinToString("\n") { (v, e) -> "$v -> $e" }

    operator fun plus(others: NamedExpressionVector) = zip(others, Expression::plus)
    operator fun minus(others: NamedExpressionVector) = zip(others, Expression::minus)
    operator fun times(k: Double) = map { it * k }
    operator fun div(k: Double) = map { it / k }
    operator fun times(e: Expression) = map { it * e }
    operator fun div(e: Expression) = map { it / e }

    fun length() = expressions.values.sumBy { it `^` 2 }.let(::sqrt)

    fun substitute(from: Expression, to: Expression) =
        map { it.substitute(from, to) }

    fun substitute(others: NamedExpressionVector) =
        map { it.substitute(others) }

    fun toVector(space: VariableSpace): Vector =
        space.variables.map { expressions.getValue(it).toDouble() }.toListVector()

    private fun zip(
        others: NamedExpressionVector,
        block: (Expression, Expression) -> Expression
    ): NamedExpressionVector {
        fun zipBody(v: Variable): Expression {
            val a = expressions[v]
            val b = others.expressions[v]
            return when {
                a == null -> b!!
                b == null -> a
                else      -> block(a, b)
            }
        }

        val all = expressions.keys + others.expressions.keys
        return if (all.size > parallelism)
            all.mapParallel { it to zipBody(it) }.toMap()
                .let(::NamedExpressionVector)
        else
            all.associateWith(::zipBody)
                .let(::NamedExpressionVector)
    }

    private fun map(block: (Expression) -> Expression) =
        if (dim > parallelism)
            expressions.entries
                .mapParallel { (v, e) -> v to block(e) }.toMap()
                .let(::NamedExpressionVector)
        else
            expressions
                .mapValues { (_, e) -> block(e) }
                .let(::NamedExpressionVector)
}
