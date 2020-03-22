package org.mechdancer.symbol.core

import org.mechdancer.algebra.core.Vector

inline class Differential(val variable: Variable)
    : FactorExpression,
      BaseExpression {
    override fun d() = Constant.`0`

    override fun substitute(from: Expression, to: Expression) =
        when (from) {
            this     -> to
            variable -> to.d()
            else     -> this
        }

    override fun substitute(map: Map<out FunctionExpression, Expression>) =
        map[this]
        ?: map[variable]?.d()
        ?: this

    override fun toFunction(space: VariableSpace) = { _: Vector -> .0 }
    override fun toString() = "d$variable"
}
