package org.mechdancer.symbol.linear

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.implement.vector.toListVector
import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.Variable
import kotlin.streams.toList

/** 表达式向量 */
inline class ExpressionVector(val expressions: Map<Variable, Expression>) {
    val dim get() = expressions.size
    operator fun get(v: Variable) = expressions[v]
    operator fun get(v: Iterable<Variable>) = v.map(expressions::get)
    override fun toString() = expressions.entries.joinToString("\n") { (v, e) -> "$v -> $e" }

    private fun zip(others: ExpressionVector, block: (Expression, Expression) -> Expression) =
        ExpressionVector(expressions.mapValues { (v, e) -> others.expressions[v]?.let { block(e, it) } ?: e })

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
        if (dim >= 6)
            others.expressions.entries.fold(expressions.values) { r, (v, e) ->
                r.parallelStream().map { e0 -> e0.substitute(v, e) }.toList()
            }.let { ExpressionVector(expressions.keys.zip(it).toMap()) }
        else
            others.expressions.entries.fold(expressions) { r, (v, e) ->
                r.mapValues { (_, e0) -> e0.substitute(v, e) }
            }.let(::ExpressionVector)

    fun toVector(space: VariableSpace): Vector =
        space.variables.map { expressions[it]?.toDouble() ?: .0 }.toListVector()

    fun toFunction(space: VariableSpace): (Vector) -> Vector {
        val list = space.variables.map { expressions[it]?.toFunction(space.variables) ?: { .0 } }
        return { v ->
            if (dim >= 6)
                list.parallelStream().mapToDouble { it(v) }.toList().toListVector()
            else
                list.map { it(v) }.toListVector()
        }
    }
}
