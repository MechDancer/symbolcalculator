package org.mechdancer.symbol.core

import org.mechdancer.algebra.core.Vector

/** 名为 [name] 的变量 */
inline class Variable(val name: String)
    : FactorExpression,
      BaseExpression,
      ExponentialExpression,
      LnExpression,
      Comparable<Variable> {
    override fun d() =
        Differential(this)

    override fun substitute(from: Expression, to: Expression) =
        if (this == from) to else this

    override fun toFunction(space: VariableSpace) =
        space.variables.indexOf(this).let { { v: Vector -> v[it] } }

    override fun compareTo(other: Variable) =
        name.compareTo(other.name)

    override fun toString() =
        name
}
