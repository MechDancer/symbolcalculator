package org.mechdancer.v2

import kotlin.math.ln
import kotlin.math.pow

/** 基本初等函数 */
sealed class BasicElement : Expression

/** 幂函数 */
class Power private constructor(
    val v: Variable,
    val c: Constant
) : BasicElement() {
    override fun d(v: Variable) =
        if (this.v == v) org.mechdancer.v2.Product.product(c, pow(v, Constant(c.value - 1)))
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

    override fun toString() =
        if (c.value == 1.0) "$v" else "$v^$c"

    companion object Builders {
        fun pow(v: Variable, c: Constant) =
            when (c.value) {
                .0   -> Constant(1.0)
                else -> Power(v, c)
            }
    }
}

/** 指数函数 */
class Exponential private constructor(
    val c: Constant,
    val v: Variable
) : BasicElement() {
    override fun d(v: Variable) =
        if (this.v == v) Product.product(Constant(ln(c.value)), this)
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

    companion object Builders {
        fun exp(c: Constant, v: Variable) =
            when {
                c.value <= 0   -> throw IllegalArgumentException()
                c.value == 1.0 -> c
                else           -> Exponential(c, v)
            }
    }
}

/** 对数函数 */
class Logarithm private constructor(
    val c: Constant,
    val v: Variable
) : BasicElement() {
    override fun d(v: Variable) =
        if (this.v == v) Product.product(Constant(1 / ln(c.value)), Power.pow(v, Constant(-1.0)))
        else Constant(.0)

    override fun substitute(v: Variable, c: Constant) =
        if (this.v == v) Constant(kotlin.math.log(c.value, this.c.value))
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

    companion object Builders {
        fun log(c: Constant, v: Variable) =
            when {
                c.value <= 0   -> throw IllegalArgumentException()
                c.value == 1.0 -> Constant(.0)
                else           -> Logarithm(c, v)
            }
    }
}
