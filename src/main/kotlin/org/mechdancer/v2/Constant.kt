package org.mechdancer.v2

inline class Constant(val value: Double) : Expression {
    override fun d(v: Variable) = Constant(.0)
    override fun substitute(v: Variable, c: Constant) = this
    override fun toString() = value.toString()
}
