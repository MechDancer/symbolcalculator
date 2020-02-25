package org.mechdancer.symbol.core

// {变量，幂因子，指数因子，对数因子} = {因子} ⊆ {积} ⊆ {函数}
// {变量，和式，指数因子，对数因子} = {底数子函数} ⊆ {函数}
// {变量，积式，幂因子，指数因子} = {指数子函数} ⊆ {函数}
// {变量，和式，对数因子} = {对数子函数} ⊆ {函数}

/** 函数表达式 */
internal interface FunctionExpression : Expression

/** 积表达式 */
internal interface ProductExpression : FunctionExpression

/** 因子表达式 */
internal interface FactorExpression : ProductExpression

/** 底数表达式 */
internal interface BaseExpression : FunctionExpression

/** 指数表达式 */
internal interface ExponentialExpression : FunctionExpression

/** 对数表达式 */
internal interface LnExpression : FunctionExpression

internal typealias Tex = String
