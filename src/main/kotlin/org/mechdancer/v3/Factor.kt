package org.mechdancer.v3

import org.mechdancer.v3.Constant.Companion.ln
import org.mechdancer.v3.Expression.FactorExpression
import org.mechdancer.v3.Expression.FunctionExpression

/**
 * 因子，作为积式成分而不可合并的对象
 *
 * 因子就是基本初等函数
 * 基本初等函数都是有且只有一个参数（表达式）的函数
 * 基本初等函数的加、减、乘、除、复合称作初等函数
 */
sealed class Factor(
    val member: FunctionExpression
) : FactorExpression {
    /** 判断是否基本初等函数 */
    fun isBasic() = member is Variable

    /**
     * 复合函数求导的链式法则
     *
     * 检查函数的形式，若是基本初等函数，直接求导
     * 否则采用复合函数求导的链式法则
     */
    final override fun d(v: Variable) =
        when (member) {
            v           -> df
            is Variable -> Constant.`0`
            else        -> Product(df, member d v)
        }

    /**
     * 复合函数的代入法则
     *
     * 检查函数的形式，若是基本初等函数，直接代入
     * 否则将对象代入参数表达式
     */
    final override fun substitute(v: Variable, e: Expression) =
        when (member) {
            v           -> substitute(e)
            is Variable -> this
            else        -> substitute(member.substitute(v, e))
        }

    /** 对链式法则展开一层 */
    protected abstract val df: Expression

    /** 代入构造函数 */
    protected abstract fun substitute(e: Expression): Expression

    /** 对复合函数的成分加括号 */
    protected val parameterString get() = if (isBasic()) "$member" else "($member)"
}

/** 幂因子 */
class Power private constructor(
    member: FunctionExpression,
    val exponent: Constant
) : Factor(member) {
    override val df by lazy { Builder(member, exponent - Constant.`1`) * exponent }
    override fun substitute(e: Expression) = Builder(e, exponent)

    override fun equals(other: Any?) =
        this === other || other is Power && exponent == other.exponent && member == other.member

    override fun hashCode() = member.hashCode() xor exponent.hashCode()
    operator fun component1() = member
    operator fun component2() = exponent
    override fun toString() = "$parameterString^$exponent"

    companion object Builder {
        operator fun invoke(e: Expression, exponent: Constant): Expression {
            fun simplify(f: FunctionExpression): Expression =
                when (f) {
                    is Power -> Builder(f.member, exponent pow exponent)
                    is Variable,
                    is Exponential,
                    is Ln    -> Power(f, exponent)
                    else     -> throw UnsupportedOperationException()
                }

            return when (exponent) {
                Constant.`0` -> Constant.`1`
                Constant.`1` -> e
                else         -> when (e) {
                    is Constant         -> e pow exponent
                    is FactorExpression -> simplify(e)
                    is Product          -> Product(e.factors.map(::simplify)) * (e.tail pow exponent)
                    is Sum              -> Power(e, exponent)
                    else                -> throw UnsupportedOperationException()
                }
            }
        }
    }
}

/** 指数因子 */
class Exponential private constructor(
    val base: Constant,
    member: FunctionExpression
) : Factor(member) {
    override val df by lazy { this * ln(base) }
    override fun substitute(e: Expression) = Builder(base, e)

    override fun equals(other: Any?) =
        this === other || other is Exponential && base == other.base && member == other.member

    override fun hashCode() = base.hashCode() xor member.hashCode()
    operator fun component1() = base
    operator fun component2() = member
    override fun toString() = "$base^$parameterString"

    companion object Builder {
        tailrec operator fun invoke(b: Constant, e: Expression): Expression =
            when {
                b < Constant.`0`  -> throw UnsupportedOperationException()
                b == Constant.`0` -> Constant.`0`
                b == Constant.`1` -> Constant.`1`
                else              -> when (e) {
                    is Constant    -> b pow e
                    is Variable,
                    is Power       -> invoke(b, e)
                    is Exponential -> invoke(b pow e.base, e.member)
                    is Ln          -> Power(e.member, ln(b))
                    is Product     -> Exponential(b pow e.tail, e.resetTail(Constant.`1`))
                    is Sum         -> Product(e.products.map { invoke(b, it) }) * (b pow e.tail)
                    else           -> throw UnsupportedOperationException()
                }
            }
    }
}

/** 自然对数因子 */
class Ln private constructor(
    member: FunctionExpression
) : Factor(member) {
    override val df by lazy { Power(member, Constant.`-1`) }
    override fun substitute(e: Expression) = Builder(e)
    override fun equals(other: Any?) = (this === other) || member == (other as? Ln)?.member
    override fun hashCode() = member.hashCode()
    override fun toString() = "ln$parameterString"

    companion object Builder {
        private fun simplify(f: FactorExpression): Expression =
            when (f) {
                is Power       -> Builder(f.member) * f.exponent
                is Exponential -> f.member * ln(f.base)
                is Variable,
                is Ln          -> Ln(f)
                else           -> throw UnsupportedOperationException()
            }

        operator fun invoke(e: Expression): Expression =
            when (e) {
                is Constant         -> ln(e)
                is FactorExpression -> simplify(e)
                is Product          -> Sum(e.factors.map(::simplify)) + ln(e.tail)
                is Sum              -> Ln(e)
                else                -> throw UnsupportedOperationException()
            }
    }
}
