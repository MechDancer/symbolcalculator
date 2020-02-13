package org.mechdancer.v2

inline class Variable(val name: String) : Expression {
    override fun d(v: Variable) = Constant(if (this == v) 1.0 else .0)
    override fun substitute(v: Variable, c: Constant) = if (this == v) c else this
    override fun toString() = name
}
