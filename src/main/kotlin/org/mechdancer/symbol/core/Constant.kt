package org.mechdancer.symbol.core

import org.mechdancer.algebra.core.Vector
import java.text.DecimalFormat
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.pow

/** 值为 [value] 的常数 */
inline class Constant(val value: Double) : Expression, Comparable<Constant> {
    override fun d() = `0`
    override fun substitute(from: Expression, to: Expression) = if (this == from) to else this
    override fun substitute(map: Map<out FunctionExpression, Expression>) = this
    override fun toFunction(v: Variable) = { _: Double -> value }
    override fun toFunction(space: VariableSpace) = { _: Vector -> value }
    override fun compareTo(other: Constant) = value.compareTo(other.value)
    override fun toString() = formatter.format(value)!!

    override fun plus(c: Constant) = Constant(value + c.value)
    override fun minus(c: Constant) = Constant(value - c.value)
    override fun times(c: Constant) = Constant(value * c.value)
    override fun div(c: Constant) = Constant(value / c.value)

    operator fun unaryMinus() = Constant(-value)
    infix fun pow(others: Constant) = Constant(value.pow(others.value))

    @Suppress("ObjectPropertyName")
    companion object {
        private val formatter = DecimalFormat("#.###")

        val NaN = Constant(Double.NaN)

        val zero = Constant(.0)
        val nZero = Constant(-.0)
        val one = Constant(1.0)
        val pi = Constant(PI)

        val e = Constant(E)
        val `π` = pi
        val `+∞` = Constant(Double.POSITIVE_INFINITY)
        val `-∞` = Constant(Double.NEGATIVE_INFINITY)
        val `0` get() = zero
        val `1` get() = one
        val `-1` = Constant(-1.0)

        fun ln(x: Constant) =
            Constant(ln(x.value))
    }
}
