package org.mechdancer.v3

import org.mechdancer.v3.Constant.Companion.`0`
import org.mechdancer.v3.Constant.Companion.`1`
import org.mechdancer.v3.Expression.FunctionMember
import org.mechdancer.v3.Expression.Member

/** 变量是表达式树的叶子 */
inline class Variable(private val name: String) : FunctionMember {
    override fun d(v: Variable) = if (this == v) `1` else `0`
    override fun substitute(v: Variable, m: Member) = if (this == v) m else this

    override fun plus(c: Constant) = Sum(this) + c
    override fun minus(c: Constant) = Sum(this) - c
    override fun times(c: Constant) = Sum(this) * c
    override fun div(c: Constant) = Sum(this) / c

    override fun toString() = name
}
