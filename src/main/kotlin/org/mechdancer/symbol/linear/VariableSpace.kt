package org.mechdancer.symbol.linear

import org.mechdancer.symbol.Variable

inline class VariableSpace(val variables: Set<Variable>) {
    val dim get() = variables.size

    /** 此空间上的平凡场 */
    val ordinaryField get() = Field(variables.associateWith { it })

    /** 此空间上的哈密顿算子 */
    val hamiltonian get() = Hamiltonian(this)

    operator fun plus(others: VariableSpace) =
        VariableSpace(variables + others.variables)

    operator fun minus(others: VariableSpace) =
        VariableSpace(variables - others.variables)
}
