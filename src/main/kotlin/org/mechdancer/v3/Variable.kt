//package org.mechdancer.v3
//
//import org.mechdancer.symbol.Constant.Companion.One
//import org.mechdancer.symbol.Constant.Companion.Zero
//import org.mechdancer.symbol.Power.Builder.pow
//
///** 变量是表达式树的叶子 */
//inline class Variable(private val name: String) : Expression {
//    override fun d(v: Variable) = if (this == v) One else Zero
//    override fun substitute(v: Variable, c: Constant) = if (this == v) c else this
//    override fun toString() = name
//    fun toPower() = pow(this, Constant(1.0))
//}
