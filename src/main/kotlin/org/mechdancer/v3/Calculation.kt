package org.mechdancer.v3

/** 运算 */
sealed class Calculation : FunctionExpression {
    protected abstract fun timesWithoutCheck(c: Constant): Expression
    protected abstract fun divWithoutCheck(c: Constant): Expression

    final override fun times(c: Constant) =
        when (c) {
            Constant.`0` -> Constant.`0`
            Constant.`1` -> this
            else         -> timesWithoutCheck(c)
        }

    final override fun div(c: Constant) =
        when (c) {
            Constant.`0` -> Constant.NaN
            Constant.`1` -> this
            else         -> divWithoutCheck(c)
        }
}

/** 和式 */
class Sum private constructor(
    internal val products: Set<ProductExpression>,
    internal val tail: Constant
) : Calculation(),
    BaseExpression,
    LnExpression {
    override fun d(v: Variable) =
        Builder(products.map { it d v })

    override fun substitute(v: Variable, e: Expression) =
        Builder(products.map { it.substitute(v, e) }) + tail

    override fun toString() =
        buildString {
            append(products.joinToString(" + "))
            if (tail != Constant.`0`) append(" + $tail")
        }

    override fun plus(c: Constant) = Sum(products, tail + c)
    override fun minus(c: Constant) = Sum(products, tail - c)
    override fun timesWithoutCheck(c: Constant) = Sum(products.map { (it * c) as ProductExpression }.toSet(), tail * c)
    override fun divWithoutCheck(c: Constant) = Sum(products.map { (it / c) as ProductExpression }.toSet(), tail / c)

    companion object Builder {
        operator fun invoke(vararg members: Expression) = invoke(members.asList())
        operator fun invoke(members: Collection<Expression>) =
            when (members.size) {
                0    -> throw UnsupportedOperationException()
                1    -> members.first()
                else -> TODO()
            }
    }
}

/** 积式 */
class Product private constructor(
    internal val factors: Set<FactorExpression>,
    internal val tail: Constant
) : Calculation(),
    ProductExpression,
    ExponentialExpression {
    override fun d(v: Variable): Expression =
        factors.indices
            .map { i ->
                factors
                    .mapIndexed { j, it -> if (i == j) it.d(v) else it }
                    .let(Builder::invoke)
            }
            .let(Sum.Builder::invoke) * tail

    override fun substitute(v: Variable, e: Expression) =
        Builder(factors.map { it.substitute(v, e) }) * tail

    override fun toString() =
        buildString {
            if (tail != Constant.`1`) append("$tail ")
            append(factors.joinToString(" "))
        }

    internal fun resetTail(newTail: Constant) = Product(factors, newTail)
    override fun timesWithoutCheck(c: Constant) = resetTail(tail * c)
    override fun divWithoutCheck(c: Constant) = resetTail(tail / c)

    companion object Builder {
        operator fun invoke(vararg e: Expression) = invoke(e.asList())
        operator fun invoke(e: Collection<Expression>) =
            when (e.size) {
                0    -> throw UnsupportedOperationException()
                1    -> e.first()
                else -> TODO()
            }
    }
}
