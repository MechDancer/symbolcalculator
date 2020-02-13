package org.mechdancer.v2

import org.mechdancer.v2.Power.Companion.pow
import org.mechdancer.v2.Product.Companion.product
import kotlin.math.ln
import kotlin.math.log

class Logarithm private constructor(
    val c: Constant,
    val v: Variable
) : Expression {
    override fun d(v: Variable) =
        if (this.v == v) product(Constant(1 / ln(c.value)), pow(v, Constant(-1.0)))
        else Constant(.0)

    override fun substitute(v: Variable, c: Constant) =
        if (this.v == v) Constant(log(c.value, this.c.value))
        else this

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Exponential) return false
        val (a1, v1) = other
        return c == a1 && v == v1
    }

    override fun hashCode(): Int =
        c.hashCode() xor v.hashCode()

    operator fun component1() = c
    operator fun component2() = v

    override fun toString() = "log($c)$v"

    companion object {
        fun log(c: Constant, v: Variable) =
            when {
                c.value <= 0   -> throw IllegalArgumentException()
                c.value == 1.0 -> Constant(.0)
                else           -> Logarithm(c, v)
            }
    }
}
