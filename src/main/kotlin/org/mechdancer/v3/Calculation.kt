package org.mechdancer.v3

import org.mechdancer.v3.Constant.Companion
import org.mechdancer.v3.Expression.FunctionMember
import org.mechdancer.v3.Expression.Member
import org.mechdancer.v3.Product.Builder.productOf
import org.mechdancer.v3.Sum.Builder.memberOf

/** 运算 */
sealed class Calculation : Expression

/** 和式 */
class Sum private constructor(
    internal val products: Set<Product>,
    internal val tail: Constant
) : Calculation(), FunctionMember, FactorBox {
    override fun d(v: Variable) =
        Builder(products.map { memberOf(it.d(v)) })

    override fun substitute(v: Variable, m: Member) =
        Builder(products.map { memberOf(it.substitute(v, m)) }) + tail

    override fun toString() =
        buildString {
            append(products.joinToString(" + "))
            if (tail != Constant.`0`) append(" + $tail")
        }

    override fun plus(c: Constant) = Sum(products, tail + c)
    override fun minus(c: Constant) = Sum(products, tail - c)
    override fun times(c: Constant) =
        when (c) {
            Constant.`0` -> Constant.`0`
            Constant.`1` -> this
            else         -> Sum(products.map { it * c }.toSet(), tail * c)
        }

    override fun div(c: Constant) = this * (Constant.`1` / c)

    internal fun unbox() =
        if (tail == Constant.`0` && products.size == 1) {
            val product = products.first()
            if (product.tail == Companion.`1` && product.factors.size == 1)
                product.factors.first()
            else
                product
        } else this

    companion object Builder {
        internal fun box(f: Factor) =
            Sum(setOf(productOf(Constant.`1`, f)), Constant.`0`)

        internal fun box(p: Product) =
            Sum(setOf(p), Constant.`0`)

        internal fun memberOf(e: Expression): Member =
            when (e) {
                is Member  -> e
                is Factor  -> box(e)
                is Product -> box(e)
                else       -> throw UnsupportedOperationException()
            }

        operator fun invoke(vararg members: Member) = invoke(members.asList())
        operator fun invoke(members: Collection<Member>) =
            when (members.size) {
                0    -> throw UnsupportedOperationException()
                1    -> members.first()
                else -> TODO()
            }
    }
}

/** 积式 */
internal class Product private constructor(
    val factors: Set<Factor>,
    val tail: Constant
) : Calculation(), FactorBox {
    override fun d(v: Variable): Expression =
        factors.indices
            .map { i ->
                factors
                    .mapIndexed { j, it -> if (i == j) it.d(v) else it }
                    .let(Builder::invoke)
            }
            .let(Sum.Builder::invoke) * tail

    override fun substitute(v: Variable, m: Member) =
        Builder(factors.map { it.substitute(v, m) }) * tail

    override fun toString() =
        buildString {
            if (tail != Constant.`1`) append("$tail ")
            append(factors.joinToString(" "))
        }

    fun resetTail(newTail: Constant) =
        Product(factors, newTail)

    // region 这两个短路计算函数只能由 [Sum] 调用，[Sum] 负责检查特殊常数

    operator fun times(c: Constant) = Product(factors, tail * c)
    operator fun div(c: Constant) = this * (Constant.`1` / c)

    // endregion

    companion object Builder {
        fun productOf(times: Constant, vararg factors: Factor) =
            when (factors.size) {
                0    -> throw UnsupportedOperationException()
                1    -> Product(setOf(factors.first()), times)
                else -> TODO()
            }

        operator fun invoke(vararg e: Expression) = invoke(e.asList())
        operator fun invoke(e: Collection<Expression>) =
            when (e.size) {
                0    -> throw UnsupportedOperationException()
                1    -> memberOf(e.first())
                else -> TODO()
            }
    }
}
