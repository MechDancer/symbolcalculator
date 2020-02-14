package org.mechdancer.symbol

import java.text.DecimalFormat

inline class Constant(val value: Double) : Expression {
    override fun d(v: Variable) = Constant(.0)
    override fun substitute(v: Variable, c: Constant) = this
    override fun toString(): String = formatter.format(value)

    companion object {
        private val formatter = DecimalFormat("#.#")

        val Zero = Constant(.0)
        val One = Constant(1.0)
        val NaN = Constant(Double.NaN)
    }
}
