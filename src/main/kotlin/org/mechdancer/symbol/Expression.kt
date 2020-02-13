package org.mechdancer.symbol

/**
 * 可微表达式
 */
interface Expression : Comparable<Expression> {
    /**
     * 对某变量求偏导
     */
    fun d(v: Variable): Expression

    /**
     * 为某变量赋值
     */
    fun substitute(v: Variable, c: Constant): Expression

    /**
     * 与另一表达式相乘
     */
    operator fun times(others: Expression): Expression
}
