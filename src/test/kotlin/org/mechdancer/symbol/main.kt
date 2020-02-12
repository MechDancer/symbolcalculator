package org.mechdancer.symbol

import org.mechdancer.symbol.DExpression.Companion.d

fun main() {
    val x by variable
    val e = 9 * x * x * x + 7 * x * x + -2
    val d = d(e) / d(x)

    println(e)
    println(d)

    println(e.substitute { this[x] = 1 })
    println(d.substitute { this[x] = 1 })
}
