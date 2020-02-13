package org.mechdancer.symbol

import kotlin.math.min
import kotlin.math.pow

/**
 * 幂式
 */
data class Product(val k: Double, val map: Map<Variable, Int>) : Expression {
    private val major by lazy { map.values.sum() }
    private val minor by lazy { map.values.sorted() }

    override fun d(v: Variable) =
        when (val changed = map[v]) {
            null -> Constant(.0)
            1    -> product(k, map - v)
            else -> product(k * changed, map + (v to changed - 1))
        }

    override fun substitute(v: Variable, c: Constant) =
        map[v]?.let { product(k * c.value.pow(it), map - v) } ?: this

    override fun times(others: Expression) =
        when (others) {
            is Constant ->
                product(k = k * others.value,
                        map = map)
            is Variable ->
                product(k = k,
                        map = map + (others to (map[others] ?: 0) + 1))
            is Product  ->
                product(k = k * others.k,
                        map = map.toMutableMap().apply {
                            for ((v, n) in others.map)
                                compute(v) { _, last -> (last ?: 0) + n }
                        })
            else        -> others * this
        }

    override fun compareTo(other: Expression) =
        when (other) {
            is Constant,
            is Variable -> +1
            is Product  -> {
                major.compareTo(other.major)
                    .takeIf { it != 0 }
                ?: run {
                    (0 until min(minor.size, other.minor.size))
                        .asSequence()
                        .map { minor[it].compareTo(other.minor[it]) }
                        .firstOrNull { it != 0 }
                    ?: 0
                }
            }
            else        -> -1
        }

    override fun toString() =
        buildString {
            if (k != 1.0) append("$k ")
            map.entries
                .joinToString(" ")
                { (v, n) -> if (n == 1) "$v" else "$v^$n" }
                .let(this::append)
        }

    companion object {
        fun product(k: Double, map: Map<Variable, Int>) =
            if (k == .0) Constant(.0)
            else map.filterValues { it != 0 }
                     .takeUnless(Map<*, *>::isEmpty)
                     ?.let { Product(k, it) }
                 ?: Constant(k)
    }
}
