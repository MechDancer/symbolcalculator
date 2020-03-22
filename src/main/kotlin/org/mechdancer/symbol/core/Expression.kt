package org.mechdancer.symbol.core

import org.mechdancer.algebra.core.Vector
import org.mechdancer.symbol.core.Constant.Companion.`1`

/** 可微表达式 */
interface Expression {
    /** 求表达式全微分 */
    fun d(): Expression

    /** 将 [from] 用 [to] 代换 */
    fun substitute(from: Expression, to: Expression): Expression

    /** 转化为便于计算的不具名形式 */
    fun toFunction(order: List<Variable>): (Vector) -> Double

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int

    override fun toString(): String
    fun toTex(): Tex = toString()

    // region 为与常数之间的运算提供优化机会

    operator fun plus(c: Constant): Expression = Sum[c, this]
    operator fun minus(c: Constant): Expression = Sum[-c, this]
    operator fun times(c: Constant): Expression = Product[c, this]
    operator fun div(c: Constant): Expression = Product[`1` / c, this]

    // endregion
}
