package org.mechdancer.v3

import org.mechdancer.v3.Constant.Companion.ln
import org.mechdancer.v3.Expression.FunctionMember
import org.mechdancer.v3.Expression.Member
import org.mechdancer.v3.Product.Builder.productOf
import org.mechdancer.v3.Sum.Builder.memberOf

/**
 * 因子，作为积式成分而不可合并的对象
 *
 * 因子就是基本初等函数
 * 基本初等函数都是有且只有一个参数（表达式）的函数
 * 基本初等函数的加、减、乘、除、复合称作初等函数
 */
internal sealed class Factor(
    val member: FunctionMember
) : Expression {
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
    protected abstract fun substitute(m: Member): Member

    /** 对复合函数的成分加括号 */
    protected val parameterString get() = if (isBasic()) "$member" else "($member)"
}

/** 幂因子 */
internal class Power private constructor(
    member: FunctionMember,
    val exponent: Constant
) : Factor(member) {
    override val df by lazy { Product(exponent, Builder(member, exponent - Constant.`1`)) }

    override fun substitute(m: Member) = Builder(m, exponent)

    override fun equals(other: Any?) =
        this === other
        || other is Power
        && exponent == other.exponent
        && member == other.member

    override fun hashCode() = member.hashCode() xor exponent.hashCode()
    operator fun component1() = member
    operator fun component2() = exponent
    override fun toString() = "$parameterString^$exponent"

    companion object Builder {
        operator fun invoke(m: Member, e: Constant): Member = TODO()
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
        this === other
        || other is Exponential
        && base == other.base
        && member == other.member

    override fun hashCode() = base.hashCode() xor member.hashCode()
    operator fun component1() = base
    operator fun component2() = member
    override fun toString() = "$base^$parameterString"

    companion object Builder {
        operator fun invoke(b: Constant, m: Member): Member = TODO()
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
        operator fun invoke(m: Member): Member = TODO()
    }
}
