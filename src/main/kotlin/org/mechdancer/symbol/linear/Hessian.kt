package org.mechdancer.symbol.linear

import org.mechdancer.symbol.`^-1`
import org.mechdancer.symbol.core.Differential
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.VariableSpace
import org.mechdancer.symbol.mapParallel
import org.mechdancer.symbol.times

/** 海森算子 */
inline class Hessian(val space: VariableSpace) {
    /** 求 [f] 数量场在此变量空间上的海森矩阵 */
    operator fun times(f: Expression) =
        hessian(f.d().d(), space)

    companion object {
        fun hessian(ddf: Expression, space: VariableSpace): HessianMatrix {
            val d = space.variables.map(::Differential)
            // 海森矩阵是对称矩阵，只保存、计算区下三角
            return sequence { for (r in space.variables.indices) for (c in 0..r) yield((d[r] * d[c]).`^-1`) }
                .toList()
                .mapParallel { ddf * it }
                .let(::HessianMatrix)
        }
    }
}
