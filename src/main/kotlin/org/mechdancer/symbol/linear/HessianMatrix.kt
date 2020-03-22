package org.mechdancer.symbol.linear

import org.mechdancer.algebra.core.Matrix
import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.implement.matrix.builder.listMatrixOf
import org.mechdancer.symbol.core.Differential
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.div
import kotlin.streams.toList

/** 海森矩阵 */
class HessianMatrix internal constructor(
    ddf: Expression,
    space: VariableSpace
) {
    val dim get() = toVariable.size
    private val toVariable = space.variables.toList()
    private val toIndex = toVariable.mapIndexed { i, v -> v to i }.toMap()
    private val expressions: List<Expression>

    init {
        val d = toVariable.map(::Differential)
        expressions = sequence { for (r in toVariable.indices) for (c in 0..r) yield(r to c) }
            .toList()
            .parallelStream()
            .map { (r, c) -> ddf / d[r] / d[c] }
            .toList()
    }

    operator fun get(rv: Variable, cv: Variable): Expression {
        val r = toIndex.getValue(rv)
        val c = toIndex.getValue(cv)
        return when {
            r >= c -> expressions[r * (r + 1) / 2 + c]
            else   -> expressions[c * (c + 1) / 2 + r]
        }
    }

    fun toFunction(space: VariableSpace): (Vector) -> Matrix {
        val list = expressions.map { it.toFunction(space.variables) }
        return { v ->
            val valueSave = list.parallelStream().mapToDouble { it(v) }.toList()
            listMatrixOf(dim, dim) { r, c ->
                when {
                    r >= c -> valueSave[r * (r + 1) / 2 + c]
                    else   -> valueSave[c * (c + 1) / 2 + r]
                }
            }
        }
    }
}
