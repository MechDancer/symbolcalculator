package org.mechdancer.symbol.linear

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.implement.vector.toListVector
import org.mechdancer.symbol.core.*
import org.mechdancer.symbol.sqrt
import org.mechdancer.symbol.sumBy
import org.mechdancer.symbol.times
import kotlin.streams.toList

class ExpressionVector(
    private val expressions: List<Expression>,
    private val space: VariableSpace
) : ExpressionStruct<Vector> {
    val dim get() = space.dim
    private val toIndex = space.variables.withIndex().associate { (i, v) -> v to i }

    operator fun get(v: Variable) = expressions[toIndex.getValue(v)]

    val length by lazy { sqrt(expressions.sumBy { it * it }) }

    override fun toFunction(space: VariableSpace): (Vector) -> Vector {
        val list = expressions.map { it.toFunction(space) }
        return { v ->
            if (list.size > parallelism)
                list.parallelStream().mapToDouble { it(v) }.toList().toListVector()
            else
                list.map { it(v) }.toListVector()
        }
    }
}
