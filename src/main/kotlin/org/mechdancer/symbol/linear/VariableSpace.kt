package org.mechdancer.symbol.linear

import org.mechdancer.symbol.Variable

/** 变量空间 */
inline class VariableSpace(val variables: Set<Variable>) {
    /** 空间的维度 */
    val dim get() = variables.size

    /** 此空间上的平凡场 */
    val ordinaryField get() = ExpressionVector(variables.associateWith { it })

    /** 此空间上的哈密顿算子 */
    val hamiltonian get() = Hamiltonian(this)

    /** 此空间上的海森算子 */
    val hessian get() = Hessian(this)

    /** 求变量空间的交空间 */
    operator fun times(others: VariableSpace) = VariableSpace(variables intersect others.variables)

    /** 求变量空间的并空间 */
    operator fun plus(others: VariableSpace) = VariableSpace(variables + others.variables)

    /** 求变量空间的差空间 */
    operator fun minus(others: VariableSpace) = VariableSpace(variables - others.variables)
}
