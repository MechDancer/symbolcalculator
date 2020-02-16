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

private typealias SumCollector = MutableMap<Expression, Double>

/** 和式 */
class Sum private constructor(
    internal val products: Set<ProductExpression>,
    internal val tail: Constant
) : Calculation(),
    BaseExpression,
    LnExpression {
    override fun d(v: Variable) = get(products.map { it d v })
    override fun substitute(v: Variable, e: Expression) = get(products.map { it.substitute(v, e) }) + tail

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
        operator fun get(vararg members: Expression) = get(members.asList())
        operator fun get(members: Collection<Expression>) =
            when (members.size) {
                0    -> throw UnsupportedOperationException()
                1    -> members.first()
                else -> {
                    val collector = mutableMapOf<Expression, Double>()
                    for (e in members) collector += e
                    val tail = collector.remove(Constant.`1`) ?: .0
                    val products = collector
                        .map { (e, k) -> Product[e, Constant(k)] as ProductExpression }
                        .toSet()
                    when {
                        products.isEmpty()               -> Constant(tail)
                        tail == .0 && products.size == 1 -> products.first()
                        else                             -> Sum(products, Constant(tail))
                    }
                }
            }

        private fun SumCollector.merge(e: Expression, b: Double) {
            compute(e) { _, a -> ((a ?: .0) + b).takeIf { it != .0 } }
        }

        private operator fun SumCollector.plusAssign(e: Expression): Unit =
            when (e) {
                Constant.`0`        -> Unit
                is Constant         -> merge(Constant.`1`, e.value)
                is FactorExpression -> merge(e, 1.0)
                is Product          -> merge(e.resetTail(Constant.`1`), e.tail.value)
                is Sum              -> {
                    for (p in e.products) this += p
                    this += e.tail
                }
                else                -> throw UnsupportedOperationException()
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
                    .let(Builder::get)
            }
            .let(Sum.Builder::get) * tail

    override fun substitute(v: Variable, e: Expression) =
        get(factors.map { it.substitute(v, e) }) * tail

    override fun toString() =
        buildString {
            if (tail != Constant.`1`) append("$tail ")
            append(factors.joinToString(" "))
        }

    internal fun resetTail(newTail: Constant) = Product(factors, newTail)
    override fun timesWithoutCheck(c: Constant) = resetTail(tail * c)
    override fun divWithoutCheck(c: Constant) = resetTail(tail / c)

    override fun equals(other: Any?) =
        this === other || other is Product && tail == other.tail && factors == other.factors

    override fun hashCode() =
        factors.hashCode() xor tail.hashCode()

    companion object Builder {
        operator fun get(vararg e: Expression) = get(e.asList())
        operator fun get(e: Collection<Expression>) =
            when (e.size) {
                0    -> throw UnsupportedOperationException()
                1    -> e.first()
                else -> TODO()
            }
    }
}
