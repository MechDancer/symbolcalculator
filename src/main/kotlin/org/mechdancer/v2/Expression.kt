package org.mechdancer.v2

interface Expression {
    fun d(v: Variable): Expression
    fun substitute(v: Variable, c: Constant): Expression
}

class Sum private constructor(
    val list: List<Expression>,
    val c: Constant
) : Expression {
    override fun d(v: Variable) =
        sum(list.map { it.d(v) })

    override fun substitute(v: Variable, c: Constant) =
        sum(list.map { it.substitute(v, c) } + c)

    override fun toString() =
        buildString {
            append(list.joinToString(" + "))
            if (c.value != .0) append(" + $c")
        }

    companion object {
        fun sum(list: Iterable<Expression>) = sum(list.asSequence())
        fun sum(vararg list: Expression) = sum(list.asSequence())
        fun sum(sequence: Sequence<Expression>): Expression = TODO()
    }
}

class Product private constructor(
    val list: List<Expression>,
    val c: Constant
) : Expression {
    override fun d(v: Variable) =
        product(c,
                list.indices
                    .asSequence()
                    .map { i ->
                        list.asSequence()
                            .mapIndexed { j, it -> if (i == j) it.d(v) else it }
                            .let(Companion::product)
                    }
                    .let(Sum.Companion::sum))

    override fun substitute(v: Variable, c: Constant) =
        product(list.map { it.substitute(v, c) } + c)

    override fun toString() =
        buildString {
            if (c.value != 1.0) append("$c ")
            append(list.joinToString(" "))
        }

    companion object {
        fun product(list: Iterable<Expression>) = product(list.asSequence())
        fun product(vararg list: Expression) = product(list.asSequence())
        fun product(sequence: Sequence<Expression>): Expression = TODO()
    }
}
