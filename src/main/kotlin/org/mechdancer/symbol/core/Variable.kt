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

    override fun toFunction(order: List<Variable>): (Vector) -> Double =
        order.indexOf(this).let { i -> { it[i] } }

    override fun compareTo(other: Variable) =
        name.compareTo(other.name)

    override fun toString() =
        name
}
