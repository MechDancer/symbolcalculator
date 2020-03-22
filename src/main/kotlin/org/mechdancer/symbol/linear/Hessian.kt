package org.mechdancer.symbol.linear

import org.mechdancer.symbol.core.*
import kotlin.streams.toList

/** 海森算子 */
inline class Hessian(val space: VariableSpace) {
    /** 求 [f] 数量场在此变量空间上的海森矩阵 */
    operator fun times(f: Expression) =
        hessian(f.d().d(), space)

    companion object {
        fun hessian(ddf: Expression, space: VariableSpace): HessianMatrix {
            val d = space.variables.map(::Differential)
            // 海森矩阵是对称矩阵，只保存、计算区下三角
            return sequence { for (r in space.variables.indices) for (c in 0..r) yield(r to c) }
                .toList()
                .parallelStream()
                .map { (r, c) -> Product[ddf, Power[d[r], Constant.`-1`], Power[d[c], Constant.`-1`]] }
                .toList()
                .let(::HessianMatrix)
        }
    }
}
