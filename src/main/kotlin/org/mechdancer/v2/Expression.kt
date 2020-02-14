package org.mechdancer.v2

/** 可微表达式 */
interface Expression {
    fun d(v: Variable): Expression
    fun substitute(v: Variable, c: Constant): Expression
}
