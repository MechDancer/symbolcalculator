@file:Suppress("MemberVisibilityCanBePrivate")

package org.mechdancer.symbol.core

import org.mechdancer.symbol.core.Constant.Companion.`0`
import org.mechdancer.symbol.core.Constant.Companion.`1`
import org.mechdancer.symbol.core.Constant.Companion.`-1`
import org.mechdancer.symbol.core.Constant.Companion.ln

/**
 * 因子，作为积式成分而不可合并的对象
 *
 * 因子就是基本初等函数
 * 基本初等函数都是有且只有一个参数（表达式）的函数
 * 基本初等函数的加、减、乘、除、复合称作初等函数
 */
sealed class Factor : FactorExpression {
    /** 初等函数都是一元函数，有且只有一个初等函数参数 */
    internal abstract val member: FunctionExpression

    /** 判断是否基本初等函数 */
    val isBasic
        get() = member is Variable

    override fun d() = Product[df, member.d()]

    /**
     * 复合函数的代入法则
     *
     * 检查函数的形式，基本初等函数直接代入，否则展开代入
     */
    final override fun substitute(from: Expression, to: Expression) =
        when (from) {
            this   -> to
            member -> substitute(to)
            else   -> substitute(member.substitute(from, to))
        }

    /** 对链式法则展开一层 */
    protected abstract val df: Expression

    /** 代入构造函数 */
    protected abstract fun substitute(e: Expression): Expression

    /** 对复合函数的成分加括号 */
    protected val parameterString get() = if (isBasic) "$member" else "($member)"

    /** 对复合函数的成分加括号 */
    protected val parameterTex get() = if (isBasic) member.toTex() else "(${member.toTex()})"
}

/** 幂因子 */
class Power private constructor(
    override val member: BaseExpression,
    val exponent: Constant
) : Factor(),
    ExponentialExpression {
    init {
        // 作为导数算子，阶数只能是整数
        if (member is Differential) require(exponent.value == exponent.value.toInt().toDouble())
    }

    override val df by lazy { get(member, exponent - `1`) * exponent }
    override fun substitute(e: Expression) = get(e, exponent)
    override fun equals(other: Any?) =
        this === other || other is Power && exponent == other.exponent && member == other.member

    override fun hashCode() = member.hashCode() xor exponent.hashCode()
    override fun toString() = "$parameterString^$exponent"
    override fun toTex(): Tex =
        when (exponent) {
            Constant(.5)  -> "\\sqrt{${member.toTex()}}"
            Constant(-.5) -> "\\frac{1}{\\sqrt{${member.toTex()}}}"
            else          -> "{$parameterTex}^{${exponent.toTex()}}"
        }

    companion object Builder {
        operator fun get(b: Expression, e: Constant): Expression {
            fun simplify(f: FunctionExpression): Expression =
                when (f) {
                    is BaseExpression -> Power(f, e)
                    is Power          -> get(f.member, f.exponent * e)
                    is Exponential    -> Exponential[f.base, get(f.member, e)]
                    else              -> throw UnsupportedOperationException()
                }

            return when (e) {
                `0`  -> `1`
                `1`  -> b
                else -> when (b) {
                    `0`                 -> `0`
                    is Constant         -> b pow e
                    is FactorExpression -> simplify(b)
                    is Product          -> Product[b.factors.map(::simplify)] * (b.times pow e)
                    is Sum              -> Power(b, e)
                    else                -> throw UnsupportedOperationException()
                }
            }
        }
    }
}

/** 指数因子 */
class Exponential private constructor(
    val base: Constant,
    override val member: ExponentialExpression
) : Factor(), BaseExpression,
    ExponentialExpression {
    override val df by lazy { this * ln(base) }
    override fun substitute(e: Expression) = get(base, e)

    override fun equals(other: Any?) =
        this === other || other is Exponential && base == other.base && member == other.member

    override fun hashCode() = base.hashCode() xor member.hashCode()
    override fun toString() = "$base^$parameterString"
    override fun toTex(): Tex = "{${base.toTex()}}^{${member.toTex()}}"

    companion object Builder {
        operator fun get(b: Constant, e: Expression): Expression =
            when {
                b < `0`  -> throw IllegalArgumentException()
                b == `0` -> `0`
                b == `1` -> `1`
                else     -> when (e) {
                    is Constant              -> b pow e
                    is Product               -> Exponential(b pow e.times, e.resetTimes(`1`))
                    is ExponentialExpression -> Exponential(b, e)
                    is Ln                    -> Power[e.member, ln(b)]
                    is Sum                   -> Product[e.products.map {
                        get(b, it)
                    }] * (b pow e.tail)
                    else                     -> throw UnsupportedOperationException()
                }
            }
    }
}

/** 自然对数因子 */
class Ln private constructor(
    override val member: LnExpression
) : Factor(), BaseExpression,
    LnExpression {
    override val df by lazy { Power[member, `-1`] }
    override fun substitute(e: Expression) = get(e)
    override fun equals(other: Any?) = this === other || other is Ln && member == other.member
    override fun hashCode() = member.hashCode()
    override fun toString() = "ln$parameterString"
    override fun toTex(): Tex = "\\ln $parameterTex"

    companion object Builder {
        private fun simplify(f: FactorExpression) =
            when (f) {
                is LnExpression -> Ln(f)
                is Power        -> get(f.member) * f.exponent
                is Exponential  -> f.member * ln(f.base)
                else            -> throw UnsupportedOperationException()
            }

        operator fun get(e: Expression): Expression =
            when (e) {
                is Constant         -> ln(e)
                is FactorExpression -> simplify(e)
                is Product          -> Sum[e.factors.map(Builder::simplify)] + ln(e.times)
                is Sum              -> Ln(e)
                else                -> throw UnsupportedOperationException()
            }

        operator fun get(base: Constant, e: Expression): Expression =
            when {
                base <= `0` || base == `1` -> throw IllegalArgumentException()
                else                       -> get(e) / ln(base)
            }
    }
}
