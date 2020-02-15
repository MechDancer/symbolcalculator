package org.mechdancer.v3

import org.mechdancer.v3.Constant.Companion.`1`

/** 可微表达式 */
interface Expression {
    infix fun d(v: Variable): Expression
    fun substitute(v: Variable, e: Expression): Expression

    operator fun plus(c: Constant): Expression = Sum(c, this)
    operator fun minus(c: Constant): Expression = Sum(-c, this)
    operator fun times(c: Constant): Expression = Product(c, this)
    operator fun div(c: Constant): Expression = Product(`1` / c, this)
}
