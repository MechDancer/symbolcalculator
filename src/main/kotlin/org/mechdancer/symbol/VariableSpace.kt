package org.mechdancer.symbol

inline class VariableSpace(val variables: Set<Variable>) {
    val dim get() = variables.size
    val ordinaryField get() = Field(variables.associateWith { it })

    fun gradientOf(f: Expression) =
        Field(variables.associateWith { f d it })
}
