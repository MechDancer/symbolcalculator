package org.mechdancer.symbol

/**
 * 常量表达式
 */
inline class Constant(val value: Double) : Expression {
    override fun d(v: Variable) = Constant(.0)
    override fun substitute(v: Variable, c: Constant) = this
    override fun times(others: Expression) =
        when (others) {
            is Constant -> Constant(value * others.value)
            else        -> others * this
        }

    override fun toString() = value.toString()
}
