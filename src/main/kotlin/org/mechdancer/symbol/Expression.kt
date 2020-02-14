package org.mechdancer.symbol

/** 可微表达式 */
interface Expression {
    fun d(v: Variable): Expression
    fun substitute(v: Variable, c: Constant): Expression
}
