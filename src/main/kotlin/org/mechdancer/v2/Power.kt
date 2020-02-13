package org.mechdancer.v2

import org.mechdancer.v2.Product.Companion.product
import kotlin.math.pow

class Power private constructor(
    val v: Variable,
    val c: Constant
) : Expression {
    override fun d(v: Variable) =
        if (this.v == v) product(c, pow(v, Constant(c.value - 1)))
        else Constant(.0)

    override fun substitute(v: Variable, c: Constant) =
        if (this.v == v) Constant(c.value.pow(this.c.value))
        else this

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Power) return false
        val (v1, a1) = other
        return v == v1 && c == a1
    }

    override fun hashCode(): Int =
        v.hashCode() xor c.hashCode()

    operator fun component1() = v
    operator fun component2() = c

    override fun toString() = "$v^$c"

    companion object {
        fun pow(v: Variable, c: Constant) =
            when (c.value) {
                0.0  -> Constant(1.0)
                1.0  -> v
                else -> Power(v, c)
            }
    }
}
