package org.mechdancer.symbol.core

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

    override fun toString() = "d$variable"
    override fun toTex() = "d${variable.toTex()}"
}
