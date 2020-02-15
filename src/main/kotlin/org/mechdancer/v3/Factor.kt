package org.mechdancer.v3

import org.mechdancer.v3.Constant.Companion
import org.mechdancer.v3.Constant.Companion.ln
import org.mechdancer.v3.Expression.FunctionMember
import org.mechdancer.v3.Expression.Member
import org.mechdancer.v3.Product.Builder.productOf
import org.mechdancer.v3.Sum.Builder.box
import org.mechdancer.v3.Sum.Builder.memberOf

internal interface FactorBox : Expression

/**
 * 因子，作为积式成分而不可合并的对象
 *
 * 因子就是基本初等函数
 * 基本初等函数都是有且只有一个参数（表达式）的函数
 * 基本初等函数的加、减、乘、除、复合称作初等函数
 */
internal sealed class Factor(
    val member: FunctionMember
) : FactorBox {
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
            is Sum      -> Product(df, member d v)
            else        -> throw UnsupportedOperationException()
        }

    /**
     * 复合函数的代入法则
     *
     * 检查函数的形式，若是基本初等函数，直接代入
     * 否则将对象代入参数表达式
     */
    final override fun substitute(v: Variable, m: Member) =
        when (member) {
            v           -> substitute(m)
            is Variable -> this
            else        -> substitute(member.substitute(v, m))
        }

    /** 对链式法则展开一层 */
    protected abstract val df: Member

    /** 代入构造函数 */
    protected abstract fun substitute(m: Member): Expression

    /** 对复合函数的成分加括号 */
    protected val parameterString get() = if (isBasic()) "$member" else "($member)"
}

/** 幂因子 */
internal class Power private constructor(
    member: FunctionMember,
    val exponent: Constant
) : Factor(member) {
    override val df by lazy { Product(exponent, memberOf(Builder(member, exponent - Constant.`1`))) }

    override fun substitute(m: Member) = Builder(m, exponent)

    override fun equals(other: Any?) =
        this === other || other is Power && exponent == other.exponent && member == other.member

    override fun hashCode() = member.hashCode() xor exponent.hashCode()
    operator fun component1() = member
    operator fun component2() = exponent
    override fun toString() = "$parameterString^$exponent"

    companion object Builder {
        private fun Factor.simplify(e: Constant): Expression =
            when (this) {
                is Power -> Builder(member, exponent pow e)
                is Exponential,
                is Ln    -> Power(box(this), e)
            }

        operator fun invoke(m: Member, e: Constant) =
            when (e) {
                Constant.`0` -> Constant.`1`
                Constant.`1` -> m
                else         -> when (m) {
                    is Constant -> m pow e
                    is Variable -> Power(m, e)
                    is Sum      -> when (val box = m.unbox()) {
                        is Factor  -> box.simplify(e)
                        is Product -> Product(box.factors.map { it.simplify(e) }) * (box.tail pow e)
                        is Sum     -> Power(box, e)
                        else       -> throw UnsupportedOperationException()
                    }
                    else        -> throw UnsupportedOperationException()
                }
            }
    }
}

/** 指数因子 */
internal class Exponential private constructor(
    val base: Constant,
    member: FunctionMember
) : Factor(member) {
    override val df by lazy { memberOf(productOf(ln(base), this)) }
    override fun substitute(m: Member) = Builder(base, m)

    override fun equals(other: Any?) =
        this === other || other is Exponential && base == other.base && member == other.member

    override fun hashCode() = base.hashCode() xor member.hashCode()
    operator fun component1() = base
    operator fun component2() = member
    override fun toString() = "$base^$parameterString"

    companion object Builder {
        private fun Factor.simplify(base: Constant): Expression =
            when (this) {
                is Power       -> Exponential(base, box(this))
                is Exponential -> Builder(base pow base, member)
                is Ln          -> Power(member, ln(base))
            }

        operator fun invoke(b: Constant, m: Member): Expression =
            when {
                b < Constant.`0`  -> throw UnsupportedOperationException()
                b == Constant.`0` -> Constant.`0`
                b == Constant.`1` -> Constant.`1`
                else              -> when (m) {
                    is Constant -> b pow m
                    is Variable -> Exponential(b, m)
                    is Sum      -> when (val box = m.unbox()) {
                        is Factor  -> box.simplify(b)
                        is Product -> Exponential(b pow box.tail, box(box.resetTail(Companion.`1`)))
                        is Sum     -> Product(m.products.map { Builder(b, box(it)) }) * (b pow m.tail)
                        else       -> throw UnsupportedOperationException()
                    }
                    else        -> throw UnsupportedOperationException()
                }
            }
    }
}

/** 自然对数因子 */
internal class Ln private constructor(
    member: FunctionMember
) : Factor(member) {
    override val df by lazy { memberOf(Power(member, Constant.`-1`)) }
    override fun substitute(m: Member) = Builder(m)
    override fun equals(other: Any?) = (this === other) || member == (other as? Ln)?.member
    override fun hashCode() = member.hashCode()
    override fun toString() = "ln$parameterString"

    companion object Builder {
        private fun Factor.simplify(): Expression =
            when (this) {
                is Power       -> memberOf(Builder(member)) * exponent
                is Exponential -> member * ln(base)
                is Ln          -> Ln(box(this))
            }

        operator fun invoke(m: Member): Expression =
            when (m) {
                is Constant -> ln(m)
                is Variable -> Ln(m)
                is Sum      -> when (val box = m.unbox()) {
                    is Factor  -> box.simplify()
                    is Product -> Sum(box.factors.map { memberOf(it.simplify()) }) + ln(box.tail)
                    is Sum     -> Ln(box)
                    else       -> throw UnsupportedOperationException()
                }
                else        -> throw UnsupportedOperationException()
            }
    }
}
