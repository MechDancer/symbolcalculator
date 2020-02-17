package org.mechdancer.symbol

import org.mechdancer.symbol.Constant.Companion.`0`
import org.mechdancer.symbol.Constant.Companion.`1`
import org.mechdancer.symbol.Constant.Companion.`-1`
import org.mechdancer.symbol.Constant.Companion.ln

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
    @Suppress("MemberVisibilityCanBePrivate")
    fun isBasic() = member is Variable

    /**
     * 复合函数求导的链式法则
     *
     * 检查函数的形式，基本初等函数直接求导，否则采用复合函数求导的链式法则
     */
    final override fun d(v: Variable) =
        when (member) {
            v           -> df
            is Variable -> `0`
            else        -> Product[df, member d v]
        }

    /**
     * 复合函数的代入法则
     *
     * 检查函数的形式，基本初等函数直接代入，否则展开代入
     */
    final override fun substitute(from: Expression, to: Expression) =
        if (member == from) substitute(to) else substitute(member.substitute(from, to))

    /** 对链式法则展开一层 */
    protected abstract val df: Expression

    /** 代入构造函数 */
    protected abstract fun substitute(e: Expression): Expression

    /** 对复合函数的成分加括号 */
    protected val parameterString get() = if (isBasic()) "$member" else "($member)"
}

/** 幂因子 */
class Power private constructor(
    override val member: BaseExpression,
    val exponent: Constant
) : Factor(), ExponentialExpression {
    override val df by lazy { get(member, exponent - `1`) * exponent }
    override fun substitute(e: Expression) = get(e, exponent)

    override fun equals(other: Any?) =
        this === other || other is Power && exponent == other.exponent && member == other.member

    override fun hashCode() = member.hashCode() xor exponent.hashCode()
    internal operator fun component1() = member
    internal operator fun component2() = exponent
    override fun toString() = "$parameterString^$exponent"

    companion object Builder {
        operator fun get(b: Expression, e: Constant): Expression {
            fun simplify(f: FunctionExpression): Expression =
                when (f) {
                    is BaseExpression -> Power(f, e)
                    is Power          -> get(f.member, e pow e)
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
) : Factor(), BaseExpression {
    override val df by lazy { this * ln(base) }
    override fun substitute(e: Expression) = get(base, e)

    override fun equals(other: Any?) =
        this === other || other is Exponential && base == other.base && member == other.member

    override fun hashCode() = base.hashCode() xor member.hashCode()
    internal operator fun component1() = base
    internal operator fun component2() = member
    override fun toString() = "$base^$parameterString"

    companion object Builder {
        tailrec operator fun get(b: Constant, e: Expression): Expression =
            @Suppress("NON_TAIL_RECURSIVE_CALL")
            when {
                b < `0`  -> throw IllegalArgumentException()
                b == `0` -> `0`
                b == `1` -> `1`
                else     -> when (e) {
                    is Constant    -> b pow e
                    is Variable    -> Exponential(b, e)
                    is Power       -> Exponential(b, e)
                    is Exponential -> get(b pow e.base, e.member)
                    is Ln          -> Power[e.member, ln(b)]
                    is Product     -> Exponential(b pow e.times, e.resetTimes(`1`))
                    is Sum         -> Product[e.products.map { get(b, it) }] * (b pow e.tail)
                    else           -> throw UnsupportedOperationException()
                }
            }
    }
}

/** 自然对数因子 */
class Ln private constructor(
    override val member: LnExpression
) : Factor(), BaseExpression, LnExpression {
    override val df by lazy { Power[member, `-1`] }
    override fun substitute(e: Expression) = get(e)
    override fun equals(other: Any?) = this === other || other is Ln && member == other.member
    override fun hashCode() = member.hashCode()
    override fun toString() = "ln$parameterString"

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
                is Product          -> Sum[e.factors.map(::simplify)] + ln(e.times)
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
