package org.mechdancer.symbol

import org.mechdancer.symbol.Constant.Companion.`0`
import org.mechdancer.symbol.Constant.Companion.`1`

/** 运算 */
sealed class Calculation : FunctionExpression {
    protected abstract fun timesWithoutCheck(c: Constant): Expression
    protected abstract fun divWithoutCheck(c: Constant): Expression

    final override fun times(c: Constant) =
        when (c) {
            `0`  -> `0`
            `1`  -> this
            else -> timesWithoutCheck(c)
        }

    final override fun div(c: Constant) =
        when (c) {
            `0`  -> Constant.NaN
            `1`  -> this
            else -> divWithoutCheck(c)
        }
}

// 和式合并器，不支持非顶层的 typealias，只能放在这里
private typealias SumCollector = MutableMap<Expression, Double>

// 合并算法，同类项系数相加或同底幂函数指数相加
private fun <T : Expression> MutableMap<T, Double>.merge(e: T, b: Double) {
    compute(e) { _, a -> ((a ?: .0) + b).takeIf { it != .0 } }
}

/** 和式 */
class Sum private constructor(
    internal val products: Set<ProductExpression>,
    internal val tail: Constant
) : Calculation(),
    BaseExpression,
    LnExpression {
    override fun d(v: Variable) = get(products.map { it d v })
    override fun substitute(v: Variable, e: Expression) = get(products.map { it.substitute(v, e) }) + tail

    override fun equals(other: Any?) =
        this === other || other is Sum && tail == other.tail && products == other.products

    override fun hashCode() = products.hashCode() xor tail.hashCode()

    override fun toString() =
        buildString {
            append(products.first())
            for (item in products.asSequence().drop(1))
                append(
                    if (item is Product && item.times < `0`)
                        " - ${item.toString().drop(1)}"
                    else " + $item"
                )
            when {
                tail > `0` -> append(" + $tail")
                tail < `0` -> append(" - ${-tail}")
            }
        }

    override fun toTex(): Tex =
        buildString {
            append(products.first().toTex())
            for (item in products.asSequence().drop(1))
                append(
                    if (item is Product && item.times < `0`)
                        " - ${item.toTex().drop(1)}"
                    else " + ${item.toTex()}"
                )
            when {
                tail > `0` -> append(" + ${tail.toTex()}")
                tail < `0` -> append(" - ${(-tail).toTex()}")
            }
        }

    override fun plus(c: Constant) = Sum(products, tail + c)
    override fun minus(c: Constant) = Sum(products, tail - c)
    override fun timesWithoutCheck(c: Constant) = Sum(products.map { (it * c) as ProductExpression }.toSet(), tail * c)
    override fun divWithoutCheck(c: Constant) = Sum(products.map { (it / c) as ProductExpression }.toSet(), tail / c)

    companion object Builder {
        operator fun get(vararg e: Expression) = get(e.asList())
        operator fun get(list: Collection<Expression>) =
            when (list.size) {
                0    -> throw UnsupportedOperationException()
                1    -> list.first()
                else -> {
                    val collector = mutableMapOf<Expression, Double>()
                    for (e in list) collector += e
                    val tail = collector.remove(`1`) ?: .0
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

        private operator fun SumCollector.plusAssign(e: Expression) {
            fun inner(e: ProductExpression): Unit =
                when (e) {
                    is FactorExpression -> merge(e, 1.0) // {var, factor = {pow, exp, ln}}
                    is Product          -> merge(e.resetTimes(`1`), e.times.value)
                    else                -> throw UnsupportedOperationException()
                }

            return when (e) {
                `0`                  -> Unit
                is Constant          -> merge(`1`, e.value)
                is ProductExpression -> inner(e) // {var, product = {factor = {pow, exp, ln}, product}}
                is Sum               -> {
                    for (p in e.products) inner(p)
                    merge(`1`, e.tail.value)
                }
                else                 -> throw UnsupportedOperationException()
            }
        }
    }
}

/** 积式 */
class Product private constructor(
    internal val factors: Set<FactorExpression>,
    internal val times: Constant
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
            .let(Sum.Builder::get) * times

    override fun substitute(v: Variable, e: Expression) =
        get(factors.map { it.substitute(v, e) }) * times

    override fun equals(other: Any?) =
        this === other || other is Product && times == other.times && factors == other.factors

    override fun hashCode() =
        factors.hashCode() xor times.hashCode()

    override fun toString() =
        buildString {
            if (times != `1`) append("$times ")
            append(factors.joinToString(" "))
        }

    override fun toTex(): Tex =
        buildString {
            if (times != `1`) append("${times.toTex()} ")
            append(factors.joinToString(" ") { it.toTex() })
        }

    internal fun resetTimes(new: Constant) = Product(factors, new)
    override fun timesWithoutCheck(c: Constant) = resetTimes(times * c)
    override fun divWithoutCheck(c: Constant) = resetTimes(times / c)

    companion object Builder {
        operator fun get(vararg e: Expression) = get(e.asList())
        operator fun get(list: Collection<Expression>): Expression {
            return when (list.size) {
                0    -> throw UnsupportedOperationException()
                1    -> list.first()
                else -> {
                    val products = mutableListOf(ProductCollector())
                    for (e in list) when (e) {
                        `0`                  -> return `0`
                        is Constant          -> for (p in products) p *= e
                        is ProductExpression -> for (p in products) p *= e
                        is Sum               -> {
                            val copy = products.toList()
                            products.clear()
                            for (a in copy) {
                                for (b in e.products)
                                    products += a * b
                                if (e.tail != `0`)
                                    products += a * e.tail
                            }
                        }
                        else                 -> throw UnsupportedOperationException()
                    }
                    Sum[products.map { it.build() }]
                }
            }
        }

        private class ProductCollector private constructor(
            private var tail: Double,
            powers: Map<BaseExpression, Double>
        ) {
            private val powers = powers.toMutableMap()

            operator fun timesAssign(c: Constant) {
                tail *= c.value
            }

            operator fun timesAssign(e: ProductExpression) {
                fun inner(e: FactorExpression) =
                    when (e) {
                        is BaseExpression -> powers.merge(e, 1.0) // {var, exp, ln}
                        is Power          -> powers.merge(e.member, e.exponent.value)
                        else              -> throw UnsupportedOperationException()
                    }

                return when (e) {
                    is FactorExpression -> inner(e) // {var, factor = {pow, exp, ln}}
                    is Product          -> {
                        for (p in e.factors) inner(p)
                        tail *= e.times.value
                    }
                    else                -> throw UnsupportedOperationException()
                }
            }

            constructor() : this(1.0, emptyMap())

            operator fun times(b: Constant) = ProductCollector(tail * b.value, powers)
            operator fun times(b: ProductExpression) = ProductCollector(tail, powers).also { it *= b }

            fun build(): Expression {
                val products = powers
                    .mapNotNull { (e, k) -> Power[e, Constant(k)] as? FactorExpression }
                    .toSet()
                return when {
                    powers.isEmpty()                  -> Constant(tail)
                    tail == 1.0 && products.size == 1 -> products.first()
                    else                              -> Product(products, Constant(tail))
                }
            }
        }
    }
}
