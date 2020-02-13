package org.mechdancer.v2

import org.mechdancer.v2.Product.Companion.product
import kotlin.math.ln
import kotlin.math.pow

class Exponential private constructor(
    val c: Constant,
    val v: Variable
) : Expression {
    override fun d(v: Variable) =
        if (this.v == v) product(Constant(ln(c.value)), this)
        else Constant(.0)

    override fun substitute(v: Variable, c: Constant) =
        if (this.v == v) Constant(this.c.value.pow(c.value))
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

    override fun toString() = "$c^$v"

    companion object {
        fun exp(c: Constant, v: Variable) =
            when {
                c.value <= 0   -> throw IllegalArgumentException()
                c.value == 1.0 -> c
                else           -> Exponential(c, v)
            }
    }
}
