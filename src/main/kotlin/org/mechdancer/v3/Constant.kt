package org.mechdancer.v3

import java.text.DecimalFormat
import kotlin.math.ln
import kotlin.math.pow

/** 常数是表达式树的叶子 */
inline class Constant(val value: Double)
    : Expression, Comparable<Constant> {
    override fun d(v: Variable) = Constant(.0)
    override fun substitute(v: Variable, e: Expression) = this
    override fun compareTo(other: Constant) = value.compareTo(other.value)
    override fun toString(): String = formatter.format(value)

    override fun plus(c: Constant) = Constant(value + c.value)
    override fun minus(c: Constant) = Constant(value - c.value)
    override fun times(c: Constant) = Constant(value * c.value)
    override fun div(c: Constant) = Constant(value / c.value)

    operator fun unaryMinus() = Constant(-value)
    infix fun pow(others: Constant) = Constant(value.pow(others.value))

    @Suppress("ObjectPropertyName")
    companion object {
        private val formatter = DecimalFormat("#.#")

        val NaN = Constant(Double.NaN)

        val `0` = Constant(.0)
        val `1` = Constant(1.0)
        val `-1` = Constant(-1.0)

        fun ln(x: Constant) = Constant(ln(x.value))
    }
}