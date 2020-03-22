package org.mechdancer.symbol.linear

import org.mechdancer.algebra.core.Matrix
import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.implement.matrix.builder.listMatrixOf
import org.mechdancer.symbol.core.*
import org.mechdancer.symbol.core.Constant.Companion.`-1`
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.streams.toList

/** 海森矩阵 */
inline class HessianMatrix(
    private val expressions: List<Expression>
) : ExpressionStruct<Matrix> {
    override fun toFunction(space: VariableSpace): (Vector) -> Matrix {
        val list = expressions.map { it.toFunction(space) }
        return { v ->
            val dim = (sqrt(8 * expressions.size + 1.0).roundToInt() - 1) / 2
            val valueSave = list.parallelStream().mapToDouble { it(v) }.toList()
            listMatrixOf(dim, dim) { r, c ->
                when {
                    r >= c -> valueSave[r * (r + 1) / 2 + c]
                    else   -> valueSave[c * (c + 1) / 2 + r]
                }
            }
        }
    }

    companion object {
        fun hessian(ddf: Expression, space: VariableSpace): HessianMatrix {
            val d = space.variables.map(::Differential)
            // 海森矩阵是对称矩阵，只保存、计算区下三角
            return sequence { for (r in space.variables.indices) for (c in 0..r) yield(r to c) }
                .toList()
                .parallelStream()
                .map { (r, c) -> Product[ddf, Power[d[r], `-1`], Power[d[c], `-1`]] }
                .toList()
                .let(::HessianMatrix)
        }
    }
}
