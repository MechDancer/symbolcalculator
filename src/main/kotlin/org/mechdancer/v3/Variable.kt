package org.mechdancer.v3

import org.mechdancer.v3.Constant.Companion.`0`
import org.mechdancer.v3.Constant.Companion.`1`
import org.mechdancer.v3.Expression.FactorExpression

/** 变量是表达式树的叶子 */
inline class Variable(private val name: String) : FactorExpression {
    override fun d(v: Variable) = if (this == v) `1` else `0`
    override fun substitute(v: Variable, e: Expression) = if (this == v) e else this
    override fun toString() = name
}
