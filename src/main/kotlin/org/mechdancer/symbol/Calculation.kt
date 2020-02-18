package org.mechdancer.symbol

import org.mechdancer.symbol.Constant.Companion.`0`
import org.mechdancer.symbol.Constant.Companion.`1`
import kotlin.math.sign

/** 运算 */
sealed class Calculation : FunctionExpression {
    protected abstract fun timesWithoutCheck(c: Constant): Expression
    protected abstract fun divWithoutCheck(c: Constant): Expression
    protected abstract fun format(which: (Expression) -> String): String

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

    final override fun toString() = format(Expression::toString)
    final override fun toTex() = format(Expression::toTex)
}

// 和式合并器，不支持非顶层的 typealias，只能放在这里
private typealias SumCollector = HashMap<Expression, Double>

// 合并算法，同类项系数相加或同底幂函数指数相加
private fun <T : Expression> HashMap<T, Double>.merge(e: T, b: Double) {
    compute(e) { _, a -> ((a ?: .0) + b).takeIf { it != .0 } }
}

/** 和式 */
class Sum private constructor(
    internal val products: Set<ProductExpression>,
    internal val tail: Constant
) : Calculation(),
    BaseExpression,
    LnExpression {
    override fun d() = get(products.map(Expression::d))

    override fun substitute(from: Expression, to: Expression) =
        if (this == from) to else get(products.map { it.substitute(from, to) }) + tail

    override fun equals(other: Any?) =
        this === other || other is Sum && tail == other.tail && products == other.products

    override fun hashCode() = products.hashCode() xor tail.hashCode()

    override fun format(which: (Expression) -> String) =
        buildString {
            append(which(products.first()))
            for (item in products.asSequence().drop(1))
                if (item is Product && item.times < `0`)
                    append(" - ${which(item).drop(1)}")
                else
                    append(" + ${which(item)}")
            when {
                tail > `0` -> append(" + ${which(tail)}")
                tail < `0` -> append(" - ${which(-tail)}")
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
                    val collector = hashMapOf<Expression, Double>()
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
            fun inner(p: ProductExpression): Unit =
                when (p) {
                    is FactorExpression -> merge(p, 1.0) // {var, factor = d_, {pow, exp, ln}}
                    is Product          -> merge(p.resetTimes(`1`), p.times.value)
                    else                -> throw UnsupportedOperationException()
                }

            return when (e) {
                `0`                  -> Unit
                is Constant          -> merge(`1`, e.value)
                is ProductExpression -> inner(e) // {var, product = {factor = d_, {pow, exp, ln}, product}}
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
    override fun d(): Expression =
        factors.indices
            .map { i ->
                factors
                    .mapIndexed { j, it -> if (i == j) it.d() else it }
                    .let(Builder::get)
            }
            .let(Sum.Builder::get) * times

    override fun substitute(from: Expression, to: Expression) =
        if (this == from) to else get(factors.map { it.substitute(from, to) }) * times

    override fun equals(other: Any?) =
        this === other || other is Product && times == other.times && factors == other.factors

    override fun hashCode() = factors.hashCode() xor times.hashCode()

    override fun format(which: (Expression) -> String) =
        buildString {
            if (times != `1`) append("${which(times)} ")
            val groups = factors.groupBy { it.differentialOrder != 0 }
            groups[false]?.let { append(it.joinToString(" ", transform = which)) }
            if (groups.size == 2) append(" ")
            groups[true]?.let { append(it.joinToString(" ", transform = which)) }
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
                        `1`                  -> Unit
                        is Constant          -> products.removeIf { it *= e; it.isZero() }
                        is ProductExpression -> products.removeIf { it *= e; it.isZero() }
                        is Sum               -> {
                            val copy = products.toList()
                            products.clear()
                            for (a in copy) {
                                products += e.products.asSequence().map(a::times).filterNot(ProductCollector::isZero)
                                if (e.tail != `0`) products += a * e.tail
                            }
                        }
                        else                 -> throw UnsupportedOperationException()
                    }
                    Sum[products.map { it.build() }]
                }
            }
        }

        // 作为导数算子的阶数
        private val ProductExpression.differentialOrder
            get() = when (this) {
                is Differential -> -1
                is Power        -> asDifferential()?.second ?: 0
                else            -> 0
            }

        private class ProductCollector private constructor(
            private var tail: Double,
            powers: Map<BaseExpression, Double>
        ) {
            private val powers = HashMap(powers)
            private val differentials = powers.keys.filterIsInstance<Differential>().toHashSet()

            fun isZero() = tail == .0

            operator fun timesAssign(c: Constant) {
                tail *= c.value
            }

            operator fun timesAssign(e: ProductExpression) {
                when (e) {
                    is FactorExpression -> inner(e) // {var, d_, factor = {pow, exp, ln}}
                    is Product          -> if (e.factors.all { inner(it); tail != .0 }) tail *= e.times.value
                    else                -> throw UnsupportedOperationException()
                }
            }

            constructor() : this(1.0, emptyMap())

            operator fun times(b: Constant) = ProductCollector(tail * b.value, powers)
            operator fun times(b: ProductExpression) = ProductCollector(tail, powers).also { it *= b }

            fun build(): Expression {
                if (tail == .0) return `0`
                val products = powers.map { (e, k) -> Power[e, Constant(k)] }
                val times = Constant(tail)
                return when {
                    products.isEmpty()                -> times
                    tail == 1.0 && products.size == 1 -> products.first()
                    else                              -> {
                        val groups = products.groupBy { it is FactorExpression }
                        val product = groups[true]
                                          ?.map { it as FactorExpression }
                                          ?.toSet()
                                          ?.let { Product(it, times) }
                                      ?: times
                        groups[false]?.let { Product[Product[it], product] } ?: product
                    }
                }
            }

            // 检查求导导致的消去
            private fun check(dv: Differential) {
                val nn = powers[dv]?.sign
                when {
                    nn == null                                   -> differentials -= dv
                    differentials.all { powers[it]?.sign == nn } -> differentials += dv
                    else                                         -> tail = .0
                }
            }

            // 处理乘以因子，独立函数以避免递归
            private fun inner(e: FactorExpression) {
                when (e) {
                    is Differential   -> {
                        powers.merge(e, 1.0)
                        check(e)
                    }
                    is BaseExpression -> // {var, exp, ln}
                        powers.merge(e, 1.0)
                    is Power          -> {
                        powers.merge(e.member, e.exponent.value)
                        (e.member as? Differential)?.let(::check)
                    }
                    else              -> throw UnsupportedOperationException()
                }
            }
        }
    }
}
