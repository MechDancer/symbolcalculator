package org.mechdancer.v2

/** 基本运算 */
sealed class Calculation : Expression

/** 和式 */
class Sum private constructor(
    val list: List<Expression>,
    val c: Constant
) : Calculation() {
    override fun d(v: Variable) =
        sum(list.map { it.d(v) })

    override fun substitute(v: Variable, c: Constant) =
        sum(list.map { it.substitute(v, c) } + c)

    override fun toString() =
        buildString {
            append(list.joinToString(" + "))
            if (c.value != .0) append(" + $c")
        }

    companion object Builders {
        fun sum(list: Iterable<Expression>) = sum(list.asSequence())
        fun sum(vararg list: Expression) = sum(list.asSequence())
        fun sum(sequence: Sequence<Expression>): Expression {
            var c = .0
            val factors =
                mutableListOf<Factor>()
                    .also { sequence.flattenTo(it) }
                    .asSequence()
                    .mapNotNull {
                        val e = it.build()
                        if (e is Constant) {
                            c += e.value
                            null
                        } else
                            e
                    }
                    .toList()
            return when {
                factors.isEmpty()            -> Constant(c)
                c == .0 && factors.size == 1 -> factors.first()
                else                         -> Sum(factors, Constant(c))
            }
        }

        private fun Sequence<Expression>.flattenTo(
            factors: MutableList<Factor>
        ) {
            fun process(e: Expression) {
                val p = when (e) {
                    is Constant     -> Factor(e.value)
                    is BasicElement -> Factor(1.0).also { it.from(e) }
                    is Product      -> Factor(e.c.value).also { e.list.forEach(it::from) }
                    else            -> throw IllegalArgumentException()
                }
                if (factors.none { it merge p }) factors += p
            }

            for (item in this) when (item) {
                Constant(.0) -> Unit
                is Sum       -> {
                    process(item.c)
                    item.list.asSequence().flattenTo(factors)
                }
                else         -> process(item)
            }
        }

        private class Factor(private var c: Double) {
            private val pow = mutableSetOf<Pair<Variable, Double>>()
            private val exp = mutableSetOf<Pair<Variable, Double>>()
            private val log = mutableSetOf<Pair<Variable, Double>>()

            fun from(e: BasicElement) {
                when (e) {
                    is Power       -> pow += e.v to e.c.value
                    is Exponential -> exp += e.v to e.c.value
                    is Logarithm   -> log += e.v to e.c.value
                }
            }

            infix fun merge(others: Factor) =
                (pow == others.pow && exp == others.exp && log == others.log)
                    .also { if (it) c += others.c }

            fun build(): Expression =
                Product.product(pow.map { (v, c) -> Power.pow(v, Constant(c)) } +
                                exp.map { (v, c) -> Exponential.exp(Constant(c), v) } +
                                log.map { (v, c) -> Logarithm.log(Constant(c), v) } +
                                Constant(c))
        }
    }
}

/** 积式 */
class Product private constructor(
    val list: List<BasicElement>,
    val c: Constant
) : Expression {
    override fun d(v: Variable) =
        product(c,
                list.indices
                    .asSequence()
                    .map { i ->
                        list.asSequence()
                            .mapIndexed { j, it -> if (i == j) it.d(v) else it }
                            .let(Builders::product)
                    }
                    .let(Sum.Builders::sum))

    override fun substitute(v: Variable, c: Constant) =
        product(list.map { it.substitute(v, c) } + c)

    override fun toString() =
        buildString {
            if (c.value != 1.0) append("$c ")
            append(list.joinToString(" "))
        }

    companion object Builders {
        fun product(list: Iterable<Expression>) = product(list.asSequence())
        fun product(vararg list: Expression) = product(list.asSequence())
        fun product(sequence: Sequence<Expression>) =
            mutableListOf(mutableListOf<Factor>(Factor.C(1.0)))
                .also { sequence.flattenTo(it) }
                .mapNotNull { it.takeUnless(Collection<*>::isEmpty)?.build() }
                .let { it.singleOrNull() ?: Sum.sum(it) }

        private fun Sequence<Expression>.flattenTo(
            elements: MutableList<MutableList<Factor>>
        ) {
            for (item in this) when (item) {
                Constant(1.0) -> Unit
                is Product    ->
                    for (factors in elements) {
                        factors.process(item.c)
                        for (e in item.list) factors.process(e)
                    }
                is Sum        -> {
                    val itemsCopy = elements.toList()
                    elements.clear()
                    for (factors: MutableList<Factor> in itemsCopy) {
                        val copy = factors.toList()
                        sequence {
                            yield(item.c)
                            for (e in item.list) yield(e)
                        }.forEach { e ->
                            elements += copy.toMutableList().process(e)
                        }
                    }
                }
                else          ->
                    for (factors in elements)
                        factors.process(item)
            }
        }

        private fun MutableList<Factor>.process(e: Expression)
            : MutableList<Factor> {
            val p = when (e) {
                is Constant    -> Factor.C(e.value)
                is Power       -> Factor.Pow(e.v, e.c.value)
                is Exponential -> Factor.Exp(e.c.value, e.v)
                is Logarithm   -> Factor.Log(e.c.value, e.v)
                else           -> throw IllegalArgumentException()
            }
            if (none { it merge p }) this += p
            return this
        }

        private fun MutableList<Factor>.build()
            : Expression {
            var c = 1.0
            val elements = mapNotNull {
                val e = it.build()
                if (e is Constant) {
                    c *= e.value
                    null
                } else
                    e as BasicElement
            }
            return when {
                c == .0                        -> Constant(.0)
                elements.isEmpty()             -> Constant(c)
                c == 1.0 && elements.size == 1 -> elements.first()
                else                           -> Product(elements, Constant(c))
            }
        }

        private sealed class Factor {
            abstract infix fun merge(others: Factor): Boolean
            abstract fun build(): Expression

            class C(var value: Double) : Factor() {
                override fun merge(others: Factor) =
                    null != (others as? C)?.also { value *= it.value }

                override fun build() =
                    Constant(value)
            }

            class Pow(val v: Variable, var c: Double) : Factor() {
                override fun merge(others: Factor) =
                    null != (others as? Pow)?.takeIf { it.v == v }?.also { c += it.c }

                override fun build() =
                    Power.pow(v, Constant(c))
            }

            class Exp(var c: Double, val v: Variable) : Factor() {
                override fun merge(others: Factor) =
                    null != (others as? Exp)?.takeIf { it.v == v }?.also { c *= it.c }

                override fun build() =
                    Exponential.exp(Constant(c), v)
            }

            class Log(val c: Double, val v: Variable) : Factor() {
                override fun merge(others: Factor) = false
                override fun build() = Logarithm.log(Constant(c), v)
            }
        }
    }
}
