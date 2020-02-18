@file:Suppress("FunctionName")

package org.mechdancer.symbol

import org.mechdancer.symbol.Constant.Companion.`-1`

// 求导

fun d(e: Expression) = e.d()

// 代入

class ValueCalculator internal constructor(e: Expression) {
    var expression = e
        private set

    operator fun set(from: Expression, to: Expression) {
        expression = expression.substitute(from, to)
    }

    operator fun set(from: Expression, to: Number) {
        expression = expression.substitute(from, Constant(to.toDouble()))
    }
}

fun Expression.substitute(block: ValueCalculator.() -> Unit) =
    ValueCalculator(this).apply(block).expression

fun Expression.substitute(field: Field) =
    field.expressions.entries.fold(this) { r, (v, e) -> r.substitute(v, e) }

fun Field.substitute(field: Field) =
    field.expressions.entries.fold(expressions) { r, (v, e) ->
        r.mapValues { (_, e0) -> e0.substitute(v, e) }
    }.let(::Field)

// 表达式运算

operator fun Expression.unaryMinus() = this * `-1`
fun Expression.reciprocal() = Power[this, `-1`]
fun sqrt(e: Expression) = Power[e, Constant(.5)]

operator fun Expression.plus(others: Expression) = Sum[this, others]
operator fun Expression.minus(others: Expression) = Sum[this, -others]
operator fun Expression.times(others: Expression) = Product[this, others]
operator fun Expression.div(others: Expression) = Product[this, others.reciprocal()]

operator fun Expression.plus(n: Number) = this + Constant(n.toDouble())
operator fun Expression.minus(n: Number) = this - Constant(n.toDouble())
operator fun Expression.times(n: Number) = this * Constant(n.toDouble())
operator fun Expression.div(n: Number) = this / Constant(n.toDouble())

operator fun Number.plus(e: Expression) = e + this
operator fun Number.minus(e: Expression) = -e + this
operator fun Number.times(e: Expression) = e * this
operator fun Number.div(e: Expression) = e.reciprocal() * this

infix fun Expression.pow(n: Number) =
    if (n is Int && n < 5) Product[List(n) { this }] else Power[this, Constant(n.toDouble())]

infix fun Expression.pow(n: Constant) =
    pow((n.value.toInt() as Number).takeIf { it.toDouble() == n.value } ?: n.value)

infix fun Constant.pow(e: Expression) = Exponential[this, e]
infix fun Number.pow(e: Expression) = Exponential[Constant(toDouble()), e]

infix fun Expression.`^`(n: Number) = pow(n)
infix fun Expression.`^`(n: Constant) = pow(n)
infix fun Constant.`^`(e: Expression) = pow(e)
infix fun Number.`^`(e: Expression) = pow(e)

fun ln(e: Expression) = Ln[e]
fun log(base: Constant) = { e: Expression -> Ln[base, e] }
fun log(base: Number) = log(Constant(base.toDouble()))

// 求和求积

fun Sequence<Expression>.sum() = Sum[toList()]
fun Iterable<Expression>.sum() = Sum[toList()]
fun Collection<Expression>.sum() = Sum[this]
fun Array<Expression>.sum() = Sum[asList()]

fun Sequence<Expression>.product() = Product[toList()]
fun Iterable<Expression>.product() = Product[toList()]
fun Collection<Expression>.product() = Product[this]
fun Array<Expression>.product() = Product[asList()]

fun <T> Sequence<T>.sumBy(block: (T) -> Expression) = Sum[map(block).toList()]
fun <T> Iterable<T>.sumBy(block: (T) -> Expression) = Sum[map(block)]
fun <T> Array<T>.sumBy(block: (T) -> Expression) = Sum[map(block)]

fun <T> Sequence<T>.productBy(block: (T) -> Expression) = Product[map(block).toList()]
fun <T> Iterable<T>.productBy(block: (T) -> Expression) = Product[map(block)]
fun <T> Array<T>.productBy(block: (T) -> Expression) = Product[map(block)]
