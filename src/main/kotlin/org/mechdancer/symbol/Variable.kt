package org.mechdancer.symbol

import org.mechdancer.symbol.Constant.Companion.`0`
import org.mechdancer.symbol.Constant.Companion.`1`

/** 名为 [name] 的变量 */
inline class Variable(private val name: String)
    : FactorExpression,
      BaseExpression,
      ExponentialExpression,
      LnExpression {
    override fun d(v: Variable) = if (this == v) `1` else `0`
    override fun substitute(v: Variable, e: Expression) = if (this == v) e else this
    override fun toString() = name
    override fun toTex(): Tex = toString()
}
