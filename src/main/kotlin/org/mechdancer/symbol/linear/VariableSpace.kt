package org.mechdancer.symbol.linear

import org.mechdancer.algebra.core.Vector
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Variable

/** 变量空间 */
inline class VariableSpace(val variables: List<Variable>) {
    /** 空间的维度 */
    val dim get() = variables.size

    /** 此空间上的平凡场 */
    val ordinaryField get() = ExpressionVector(variables.associateWith { it })

    /** 此空间上的哈密顿算子 */
    val hamiltonian get() = Hamiltonian(this)

    /** 此空间上的海森算子 */
    val hessian get() = Hessian(this)

    /** 向量变为表达式向量 */
    fun order(vector: Vector) =
        variables
            .mapIndexed { i, v -> v to Constant(vector[i]) }
            .toMap()
            .let(::ExpressionVector)

    /** 求变量空间的交空间 */
    operator fun times(others: VariableSpace) =
        variables(variables.toSet() intersect others.variables.toSet())

    /** 求变量空间的并空间 */
    operator fun plus(others: VariableSpace) =
        variables(variables + others.variables)

    /** 求变量空间的差空间 */
    operator fun minus(others: VariableSpace) =
        variables(variables - others.variables)

    companion object {
        fun variables(variables: Iterable<Variable>) =
            VariableSpace(variables.toSortedSet().toList())

        fun variables(vararg names: String) =
            VariableSpace(names.toSortedSet().map(::Variable))

        fun variables(range: CharRange) =
            VariableSpace(range.map { Variable(it.toString()) })

        val xyz = variables('x'..'z')
        val characters = variables('a'..'z')
    }
}
