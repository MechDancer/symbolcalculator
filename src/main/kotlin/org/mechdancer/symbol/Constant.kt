package org.mechdancer.symbol

import java.text.DecimalFormat
import kotlin.math.ln
import kotlin.math.log
import kotlin.math.pow

inline class Constant(val value: Double)
    : Expression, Comparable<Constant> {
    override fun d(v: Variable) = Constant(.0)
    override fun substitute(v: Variable, c: Constant) = this
    override fun compareTo(other: Constant) = value.compareTo(other.value)
    override fun toString(): String = formatter.format(value)

    operator fun plus(others: Constant) = Constant(value + others.value)
    operator fun minus(others: Constant) = Constant(value - others.value)
    operator fun times(others: Constant) = Constant(value * others.value)
    operator fun div(others: Constant) = Constant(value / others.value)

    infix fun pow(others: Constant) = Constant(value.pow(others.value))

    companion object {
        private val formatter = DecimalFormat("#.#")

        val Zero = Constant(.0)
        val One = Constant(1.0)
        val NaN = Constant(Double.NaN)

        fun log(a: Constant, x: Constant) = Constant(log(x.value, a.value))
        fun ln(x: Constant) = Constant(ln(x.value))
    }
}
