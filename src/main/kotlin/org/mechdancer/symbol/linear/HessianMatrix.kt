package org.mechdancer.symbol.linear

import org.mechdancer.algebra.core.Matrix
import org.mechdancer.algebra.implement.matrix.builder.listMatrixOf
import org.mechdancer.symbol.*

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
        expressions = sequence {
            for (r in toVariable.indices)
                for (c in 0..r)
                    yield(ddf / d[r] / d[c])
        }.toList()
    }

    operator fun get(rv: Variable, cv: Variable): Expression {
        val r = toIndex.getValue(rv)
        val c = toIndex.getValue(cv)
        return when {
            r >= c -> expressions[r * (r + 1) / 2 + c]
            else   -> expressions[c * (c + 1) / 2 + r]
        }
    }

    /** 代入数值，产生数量矩阵 */
    fun toMatrix(values: ExpressionVector): Matrix {
        // 代入之后应该全是数值
        val valueSave = expressions.map { it.substitute(values).toDouble() }
        return listMatrixOf(dim, dim) { r, c ->
            when {
                r >= c -> valueSave[r * (r + 1) / 2 + c]
                else   -> valueSave[c * (c + 1) / 2 + r]
            }
        }
    }
}
