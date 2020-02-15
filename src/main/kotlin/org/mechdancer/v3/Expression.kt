package org.mechdancer.v3

/** 可微表达式 */
interface Expression {
    infix fun d(v: Variable): Expression = TODO()
    fun substitute(v: Variable, m: Member): Expression = TODO()

    /** 运算成分 */
    interface Member : Expression {
        override fun d(v: Variable): Member
        override fun substitute(v: Variable, m: Member): Member

        operator fun plus(c: Constant): Member
        operator fun minus(c: Constant): Member
        operator fun times(c: Constant): Member
        operator fun div(c: Constant): Member
    }

    /** 函数化运算成分（包含变量的运算成分） */
    interface FunctionMember : Member

}
