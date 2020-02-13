package org.mechdancer.symbol

import org.mechdancer.symbol.Product.Companion.product

/**
 * 多项式
 */
data class Multinomial(val list: List<Expression>) : Expression {
    override fun d(v: Variable) =
        list.map { it.d(v) }
            .let(Companion::multinomial)

    override fun substitute(v: Variable, c: Constant) =
        list.map { it.substitute(v, c) }
            .let(Companion::multinomial)

    override fun times(others: Expression) =
        when (others) {
            is Constant,
            is Variable,
            is Product     -> multinomial(list.map { it * others })
            is Multinomial -> multinomial(sequence { for (a in list) for (b in others.list) yield(a * b) })
            else           -> others * this
        }

    override fun compareTo(other: Expression) =
        when (other) {
            is Constant,
            is Variable,
            is Product     -> +1
            is Multinomial -> 0
            else           -> -1
        }

    override fun toString() = list.joinToString(" + ")

    companion object {
        fun multinomial(list: Iterable<Expression>): Expression =
            multinomial(list.asSequence())

        fun multinomial(vararg list: Expression): Expression =
            multinomial(list.asSequence())

        fun multinomial(list: Sequence<Expression>): Expression {
            // 展平并分类项
            val products = mutableMapOf<Map<Variable, Int>, Double>()
            val others = mutableListOf<Expression>()
            var constant = list.flatten(products, others)
            // 转化整理幂式
            val result = others.toMutableList()
            result += products.mapNotNull { (e, k) ->
                when (val p = product(k, e)) {
                    is Constant -> {
                        constant += p.value
                        null
                    }
                    else        -> p
                }
            }
            if (constant != .0)
                result += Constant(constant)
            // 形成多项式或其他表达式
            return when (result.size) {
                0    -> Constant(.0)
                1    -> result.single()
                else -> Multinomial(result)
            }
        }

        // 从所有级别的表达式中获得各项并分类
        // 修改幂式表，未知项表，返回所有常数的和
        private fun Sequence<Expression>.flatten(
            products: MutableMap<Map<Variable, Int>, Double>,
            others: MutableList<Expression>
        ): Double {
            var c = .0
            for (item in this)
                when (item) {
                    is Constant    ->
                        c += item.value
                    is Variable    ->
                        products.compute(mapOf(item to 1))
                        { _, last -> (last ?: .0) + 1 }
                    is Product     ->
                        products.compute(item.map)
                        { _, last -> (last ?: .0) + item.k }
                    is Multinomial ->
                        c += item.list.asSequence().flatten(products, others)
                    else           ->
                        others += item
                }
            return c
        }
    }
}
