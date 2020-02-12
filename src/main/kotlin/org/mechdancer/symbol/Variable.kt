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
            is Constant -> toProduct(others.value)
            is Variable -> product(1.0, mapOf(this to 1, others to 1))
            else        -> others * this
        }

    fun toProduct(k: Double = 1.0, n: Int = 1) =
        product(k, mapOf(this to n))

    override fun toString() = name
}
