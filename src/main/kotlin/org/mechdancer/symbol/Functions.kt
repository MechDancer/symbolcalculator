@file:Suppress("FunctionName", "ObjectPropertyName")

package org.mechdancer.symbol

import org.mechdancer.symbol.Constant.Companion.`-1`
import kotlin.math.E

/** 求表达式全微分 */
fun d(e: Expression) = e.d()

// 表达式四则运算

operator fun Expression.unaryMinus() = this * `-1`
val Expression.`^-1` get() = Power[this, `-1`]

operator fun Expression.plus(others: Expression) = Sum[this, others]
operator fun Expression.minus(others: Expression) = Sum[this, -others]
operator fun Expression.times(others: Expression) = Product[this, others]
operator fun Expression.div(others: Expression) = Product[this, others.`^-1`]

operator fun Expression.plus(n: Number) = this + Constant(n.toDouble())
operator fun Expression.minus(n: Number) = this - Constant(n.toDouble())
operator fun Expression.times(n: Number) = this * Constant(n.toDouble())
operator fun Expression.div(n: Number) = this / Constant(n.toDouble())

operator fun Number.plus(e: Expression) = e + this
operator fun Number.minus(e: Expression) = -e + this
operator fun Number.times(e: Expression) = e * this
operator fun Number.div(e: Expression) = e.`^-1` * this

// 幂指对构造

fun sqrt(e: Expression) = Power[e, Constant(.5)]

infix fun Expression.pow(n: Number) =
    ((n as? Int) ?: n.toInt().takeIf { it.toDouble() == n.toDouble() })
        ?.let { Product[List(it) { this }] }
    ?: Power[this, Constant(n.toDouble())]

infix fun Number.pow(e: Expression) =
    Exponential[Constant(toDouble()), e]

infix fun Expression.pow(others: Expression) =
    when (this) {
        is Constant    -> Exponential[this, others]
        is Exponential -> Exponential[base, Product[member, others]]
        else           -> when (others) {
            is Constant -> pow(others.value)
            // 对于幂指函数，取对数幂转化为基本初等函数的复合形式
            else        -> Exponential[Constant(E), Product[others, Ln[this]]]
        }
    }

infix fun Expression.`^`(n: Number) = pow(n)
infix fun Expression.`^`(e: Expression) = pow(e)
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
