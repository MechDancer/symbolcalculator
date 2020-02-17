package org.mechdancer.symbol

inline class VariableSpace(val variables: Set<Variable>) {
    val dim get() = variables.size

    fun gradientOf(f: Expression) =
        Field(variables.associateWith { f d it })
}
