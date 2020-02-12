package org.mechdancer.symbol

import org.mechdancer.symbol.DExpression.Companion.d

fun main() {
    val x = Variable("x")
    val e = 9 * x * x * x + 7 * x * x + -2
    val d = d(e) / d(x)

    println(e)
    println(d)

    println(e.substitute { this[x] = 1 })
    println(d.substitute { this[x] = 1 })
}

operator fun Number.times(e: Expression) =
    e * Constant(toDouble())

operator fun Expression.times(n: Number) =
    this * Constant(n.toDouble())

operator fun Number.plus(e: Expression) =
    e + Constant(toDouble())

operator fun Expression.plus(n: Number) =
    this + Constant(n.toDouble())
