package org.mechdancer.symbol

import org.mechdancer.symbol.Constant.Companion.One
import org.mechdancer.symbol.Constant.Companion.Zero
import org.mechdancer.symbol.Constant.Companion.ln
import org.mechdancer.symbol.Power.Builders.pow
import org.mechdancer.symbol.Product.Builders.product

/**
 * 基本初等函数
 *
 * 基本初等函数都是有且只有一个未知数的函数，互相没有转换关系
 */
sealed class BasicFunction(val v: Variable) : Expression

/** 幂函数 */
class Power private constructor(
    v: Variable,
    val c: Constant
) : BasicFunction(v) {
    override fun d(v: Variable) =
        if (this.v == v) product(c, pow(v, c - One))
        else Zero

    override fun substitute(v: Variable, c: Constant) =
        if (this.v == v) c pow this.c
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
        when (c) {
            One  -> "$v"
            else -> "$v^$c"
        }

    companion object Builders {
        fun pow(v: Variable, c: Constant) =
            when (c) {
                Zero -> One
                else -> Power(v, c)
            }
    }
}

/** 指数函数 */
class Exponential private constructor(
    val c: Constant,
    v: Variable
) : BasicFunction(v) {
    override fun d(v: Variable) =
        if (this.v == v) product(ln(c), this)
        else Zero

    override fun substitute(v: Variable, c: Constant) =
        if (this.v == v) this.c pow c
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
                c <= Zero -> throw IllegalArgumentException()
                c == One  -> c
                else      -> Exponential(c, v)
            }
    }
}

/** 对数函数 */
class Logarithm private constructor(
    val c: Constant,
    v: Variable
) : BasicFunction(v) {
    override fun d(v: Variable) =
        if (this.v == v) product(One / ln(c), pow(v, Constant(-1.0)))
        else Zero

    override fun substitute(v: Variable, c: Constant) =
        if (this.v == v) Constant.log(this.c, c)
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
                c <= Zero -> throw IllegalArgumentException()
                c == One  -> Zero
                else      -> Logarithm(c, v)
            }
    }
}
