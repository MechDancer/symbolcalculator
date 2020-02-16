package org.mechdancer.symbol

import org.mechdancer.symbol.Constant.Companion.`0`
import org.mechdancer.symbol.Constant.Companion.`1`

/** 变量是表达式树的叶子 */
inline class Variable(private val name: String)
    : FactorExpression,
      BaseExpression,
      ExponentialExpression,
      LnExpression {
    override fun d(v: Variable) = if (this == v) `1` else `0`
    override fun substitute(v: Variable, e: Expression) = if (this == v) e else this
    override fun toString() = name
}
