package org.mechdancer.symbol

import org.mechdancer.symbol.Product.Companion.product

/**
 * 变量表达式
 */
inline class Variable(private val name: String) : Expression {
    override fun d(v: Variable) =
        Constant(if (name == v.name) 1.0 else .0)

    override fun substitute(v: Variable, c: Constant) =
        if (name == v.name) c else this

    override fun times(others: Expression) =
        when (others) {
            is Constant -> product(others.value, mapOf(this to 1))
            is Variable -> product(1.0, mapOf(this to 1, others to 1))
            else        -> others * this
        }

    override fun compareTo(other: Expression) =
        when (other) {
            is Constant -> +1
            is Variable -> name.compareTo(other.name)
            else        -> -1
        }

    override fun toString() = name
}
