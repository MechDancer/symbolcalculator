//package org.mechdancer.v3
//
//import org.mechdancer.v3.Constant.Companion.`0`
//import org.mechdancer.v3.Constant.Companion.`1`
//import org.mechdancer.v3.Constant.Companion.`-1`
//import org.mechdancer.v3.Constant.Companion.ln
//import org.mechdancer.v3.Product.Builders.product
//
///**
// * 基本初等函数
// *
// * 基本初等函数都是有且只有一个参数（表达式）的函数
// * 基本初等函数的加、减、乘、除、复合称作初等函数
// */
//sealed class BasicFunction(val e: Expression) : Expression {
//    /**
//     * 复合函数求导的链式法则
//     *
//     * 检查函数的形式，若是基本初等函数，直接求导
//     * 否则采用复合函数求导的链式法则
//     */
//    final override fun d(v: Variable) =
//        when (e) {
//            v           -> df
//            is Variable -> Constant
//            else        -> product(df, e.d(v))
//        }
//
//    /** 对链式法则展开一层 */
//    protected abstract val df: Expression
//
//    /** 对复合函数的成分加括号 */
//    protected val parameterString get() = if (e is Variable) "$e" else "($e)"
//}
//
///** 幂函数 */
//class Power private constructor(
//    e: Expression,
//    val c: Constant
//) : BasicFunction(e) {
//    override val df by lazy { product(c, Builder[e, c - `1`]) }
//
//    override fun substitute(v: Variable, c: Constant) =
//        when (e) {
//            v           -> c pow this.c
//            is Variable -> this
//            else        -> Builder[e.substitute(v, c), this.c]
//        }
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other !is Power) return false
//        val (e1, c1) = other
//        return c == c1 && e == e1
//    }
//
//    override fun hashCode() = e.hashCode() xor c.hashCode()
//    operator fun component1() = e
//    operator fun component2() = c
//    override fun toString() = "$parameterString^$c"
//
//    companion object Builder {
//        tailrec operator fun get(e: Expression, c: Constant): Expression =
//            when (c) {
//                `0`  -> `1`
//                `1`  -> e
//                else -> when (e) {
//                    is Constant    -> e pow c
//                    is Power       -> get(e.e, e.c * c)
//                    is Exponential -> Exponential[e.c pow c, e.e]
//                    else           -> Power(e, c)
//                }
//            }
//
//        @Deprecated("use get", ReplaceWith("this[v, c]", "org.mechdancer.symbol.Power.Builders"))
//        fun pow(v: Variable, c: Constant) = get(v, c)
//    }
//}
//
///** 指数函数 */
//class Exponential private constructor(
//    val c: Constant,
//    e: Expression
//) : BasicFunction(e) {
//    override val df by lazy { product(ln(c), e) }
//
//    override fun substitute(v: Variable, c: Constant) =
//        when (e) {
//            v           -> this.c pow c
//            is Variable -> this
//            else        -> Builder[this.c, e.substitute(v, c)]
//        }
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other !is Exponential) return false
//        val (c1, e1) = other
//        return c == c1 && e == e1
//    }
//
//    override fun hashCode() = c.hashCode() xor e.hashCode()
//    operator fun component1() = c
//    operator fun component2() = e
//    override fun toString() = "$c^$parameterString"
//
//    companion object Builder {
//        tailrec operator fun get(c: Constant, e: Expression): Expression =
//            when {
//                c <= `0` -> throw IllegalArgumentException()
//                c == `1` -> c
//                else     -> when (e) {
//                    is Constant    -> c pow e
//                    is Power       -> get(c pow e.c, e.e)
//                    is Exponential -> get(c pow e.c, e.e)
//                    is Ln          -> Power[e.e, ln(c)]
//                    else           -> Exponential(c, e)
//                }
//            }
//
//        @Deprecated("use get", ReplaceWith("get(c, v)", "org.mechdancer.symbol.Exponential.Builder.get"))
//        fun exp(c: Constant, v: Variable) = get(c, v)
//    }
//}
//
///** 自然对数函数 */
//class Ln private constructor(
//    e: Expression
//) : BasicFunction(e) {
//    override val df by lazy { Power[e, `-1`] }
//
//    override fun substitute(v: Variable, c: Constant) =
//        when (e) {
//            v           -> ln(c)
//            is Variable -> this
//            else        -> Builder[e.substitute(v, c)]
//        }
//
//    override fun equals(other: Any?) = (this === other) || e == (other as? Ln)?.e
//    override fun hashCode() = e.hashCode()
//    override fun toString() = "ln$parameterString"
//
//    companion object Builder {
//        operator fun get(e: Expression): Expression =
//            when (e) {
//                is Constant    -> ln(e)
//                is Power       -> product(e.c, get(e.e))
//                is Exponential -> product(e.e, ln(e.c))
//                else           -> Ln(e)
//            }
//
//        operator fun get(c: Constant, e: Expression): Expression =
//            when {
//                c <= `0` -> throw IllegalArgumentException()
//                c == `1` -> `0`
//                else     -> when (e) {
//                    is Constant -> c log e
//                    else        -> product(get(e), `1` / ln(c))
//                }
//            }
//
//        @Deprecated("use get", ReplaceWith("get(c, v)", "org.mechdancer.symbol.Logarithm.Builder.get"))
//        fun log(c: Constant, v: Variable) = get(c, v)
//    }
//}
