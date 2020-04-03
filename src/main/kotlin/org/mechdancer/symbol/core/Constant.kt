package org.mechdancer.symbol.core

import org.mechdancer.algebra.core.Vector
import org.mechdancer.geometry.angle.toRad
import java.text.DecimalFormat
import kotlin.math.*

/** 实部 [re]，虚部 [im] 的常复数 */
data class Constant(val re: Double, val im: Double = .0)
    : Expression, Comparable<Constant> {
    /** 模长 */
    val norm by lazy { hypot(re, im) }

    /** 幅角 */
    val arg by lazy { atan2(im, re) }

    /** 共轭 */
    val conjugate get() = Constant(re, -im)

    override fun d() = `0`
    override fun substitute(from: Expression, to: Expression) = if (this == from) to else this
    override fun substitute(map: Map<out FunctionExpression, Expression>) = this
    override fun toFunction(v: Variable) = { _: Double -> re }
    override fun toFunction(space: VariableSpace) = { _: Vector -> re }

    override fun compareTo(other: Constant): Int {
        require(im == .0 && other.im == .0)
        return re.compareTo(other.re)
    }

    override fun toString(): String {
        val reText = format(re)
        val imText = format(im)
        return when {
            reText == "0" && imText == "0" -> "0"
            reText == "0"                  -> imText
            imText == "0"                  -> reText
            imText.startsWith('-')         -> "$reText - ${imText.drop(1)}i"
            else                           -> "$reText + ${imText}i"
        }
    }

    fun toStringAsComponent() =
        toString().let { if (" " in it) "($it)" else it }

    fun toStringAsPolar() =
        if (im == .0) format(re)
        else "${format(norm)}∠${format(arg.toRad().degree)}°"

    override fun plus(c: Constant) = Constant(re + c.re, im + c.im)
    override fun minus(c: Constant) = Constant(re - c.re, im - c.im)
    override fun times(c: Constant) = Constant(re * c.re - im * c.im, re * c.im + im * c.re)
    override fun div(c: Constant): Constant {
        val k = 1 / (c.re * c.re + c.im * c.im)
        return Constant((re * c.re + im * c.im) * k, (im * c.re - re * c.im) * k)
    }

    operator fun unaryMinus() = Constant(-re, -im)
    infix fun pow(e: Constant) =
        if (im == .0)
            polar(re.pow(e.re), if (e.im == .0) .0 else e.im * ln(re))
        else {
            val lnz = ln(norm)
            val theta = arg
            val (a, b) = e
            polar(exp(a * lnz - b * theta), a * theta + b * lnz)
        }

    @Suppress("ObjectPropertyName", "unused", "NonAsciiCharacters")
    companion object {
        private val formatter = DecimalFormat("#.###")
        private fun format(n: Double) = formatter.format(n)!!

        fun polar(norm: Double, arg: Double) =
            Constant(norm * cos(arg), norm * sin(arg))

        val NaN = Constant(Double.NaN)

        val zero = Constant(.0)
        val one = Constant(1.0)

        val i = Constant(.0, 1.0)
        val e = Constant(E)
        val π = Constant(PI)
        val `+∞` = Constant(Double.POSITIVE_INFINITY)
        val `-∞` = Constant(Double.NEGATIVE_INFINITY)
        val `0` get() = zero
        val `1` get() = one
        val `-1` = Constant(-1.0)

        fun ln(x: Constant) = Constant(ln(x.norm), x.arg)
    }
}
