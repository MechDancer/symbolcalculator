package org.mechdancer.v3

import org.mechdancer.v3.Expression.FunctionMember
import org.mechdancer.v3.Expression.Member
import org.mechdancer.v3.Product.Builder.productOf

/** 运算 */
sealed class Calculation : Expression

/** 和式 */
class Sum private constructor(
    internal val products: Set<Product>,
    internal val tail: Constant
) : Calculation(), FunctionMember {
    override fun d(v: Variable): Member = TODO()
    override fun substitute(v: Variable, m: Member): Member = TODO()

    override fun toString(): String {
        return super.toString()
    }

    companion object Builder {
        fun memberOf(e: Expression): Member =
            when (e) {
                is Member  -> e
                is Factor  -> Sum(setOf(productOf(Constant.`1`, e)), Constant.`0`)
                is Product -> Sum(setOf(e), Constant.`0`)
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
    internal val head: Constant
) : Calculation() {
    override fun d(v: Variable): Expression = TODO()
    override fun substitute(v: Variable, m: Member): Expression = TODO()

    companion object Builder {
        fun productOf(times: Constant, vararg factors: Factor) =
            when (factors.size) {
                0    -> throw UnsupportedOperationException()
                1    -> Product(setOf(factors.first()), times)
                else -> TODO()
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
