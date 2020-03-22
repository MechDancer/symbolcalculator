package org.mechdancer.symbol.linear

import org.mechdancer.symbol.core.Differential
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.VariableSpace
import org.mechdancer.symbol.div

/** 哈密顿算子（梯度算子） */
inline class Hamiltonian(private val space: VariableSpace) {
    /** 求 [f] 数量场在此变量空间上的梯度 */
    operator fun times(f: Expression) =
        gradient(f.d(), space)

    internal companion object {
        fun gradient(df: Expression, space: VariableSpace) =
            NamedExpressionVector(space.variables.associateWith {
                df / Differential(it)
            })
    }
}
