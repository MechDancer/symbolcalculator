package org.mechdancer.symbol.linear

import org.mechdancer.algebra.core.Matrix
import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.implement.matrix.builder.listMatrixOf
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.ExpressionStruct
import org.mechdancer.symbol.core.VariableSpace
import org.mechdancer.symbol.core.parallelism
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.streams.toList

/** 海森矩阵 */
inline class HessianMatrix(
    private val expressions: List<Expression>
) : ExpressionStruct<Matrix> {
    override fun toFunction(space: VariableSpace): (Vector) -> Matrix {
        val list = expressions.map { it.toFunction(space) }
        val map =
            if (list.size > parallelism)
                { v: Vector -> list.parallelStream().mapToDouble { it(v) }.toList() }
            else
                { v: Vector -> list.map { it(v) } }
        return { v ->
            val valueSave = map(v)
            val dim = (sqrt(8 * expressions.size + 1.0).roundToInt() - 1) / 2
            listMatrixOf(dim, dim) { r, c ->
                when {
                    r >= c -> valueSave[r * (r + 1) / 2 + c]
                    else   -> valueSave[c * (c + 1) / 2 + r]
                }
            }
        }
    }
}
