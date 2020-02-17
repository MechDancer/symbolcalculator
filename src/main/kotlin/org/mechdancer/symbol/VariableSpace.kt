package org.mechdancer.symbol

inline class VariableSpace(val variables: Set<Variable>) {
    val dim get() = variables.size

    /** 此空间上的平凡场 */
    val ordinaryField get() = Field(variables.associateWith { it })

    /** 求 [f] 数量场在此变量空间上的梯度 */
    fun gradientOf(f: Expression): Field {
        val df = f.d()
        return Field(variables.associateWith { df / Differential(it) })
    }

    operator fun plus(others: VariableSpace) = VariableSpace(variables + others.variables)
    operator fun minus(others: VariableSpace) = VariableSpace(variables - others.variables)
}
