package org.mechdancer.v2

import java.text.DecimalFormat

inline class Constant(val value: Double) : Expression {
    override fun d(v: Variable) = Constant(.0)
    override fun substitute(v: Variable, c: Constant) = this
    override fun toString(): String = formatter.format(value)

    private companion object {
        val formatter = DecimalFormat("#.#")
    }
}
