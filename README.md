# 符号计算器

实现了初等函数的符号运算，现已支持：
- 部分基本初等函数：常数函数、幂函数、指数函数、对数函数
- 部分初等函数：对上述基本初等函数进行加、减、乘、除和复合运算
- 上述初等函数的微分和偏导数计算
- 表达式代换

## 使用指南

### 类型

库中重要的类型包括：

- 表达式 `Expression`

  表达式是所有受支持的可微分表达式集合。

- 变量 `Variable`

  变量指示运算中的符号元素。

### 录入和构造

借助丰富的扩展函数，用户可自然地录入表达式：

* 定义变量

  ```kotlin
  val x by variable // x 是一个名为 "x" 的变量
  val y by variable // y 是一个名为 "y" 的变量
  val variable by variable // variable 是一个名为 "variable" 的变量
  val t = Variable("ttt") // t 是一个名为 "ttt" 的变量
  
  // points 是包含 {x1, y1, z1, x2, ... , z5} 这 15 个变量的变量空间
  val points by variableSpace("x", "y", "z", indices = 1..5)
  
  // xn 是包含 {xa, xb, xc} 这 3 个变量的变量空间
  val xn by variableSpace("x", indices = listOf("a", "b", "c"))
  ```

  > 变量空间：变量空间是用于求梯度的辅助类型，多元数量函数 `y = f(x, y, z, ...)` 可以看作一个空间 `{x, y, z, ...}` 上的数量场，而其在空间 `{x, y, z, ...}` 上的梯度表示为 `{∂f/∂x, ∂f/∂y, ∂f/∂z, ...}`，如果把某个变量 `x` 视作参数，也可求其在空间 `{y, z, ...}` 上的梯度 `{∂f(x)/∂y, ∂f(x)/∂z, ...}`。因此，必须指明在哪个空间上，梯度才有意义。

* 定义表达式

  下面是一些表达式的示例：

  ```kotlin
  val x by variable
  val y by variable
  
  val f1 = 9 * x * x * y + 7 * x * x + 4 * y - 2
  val f2 = ln(9 * x - 7)
  
  val f3 = (sqrt(x * x + y) + 1) `^` 2
  // `^` 是乘方的符号形式，也可写作 pow
  // 注意中缀表达式具有最低运算优先级，低于 +、-，因此作为和式、积式成分必须加括号
  ```

  只要其中至少包含一个未知数成分或其他表达式，整个对象会被自动推断为表达式。

  表达式可以在初等函数的范围内复合：

  ```kotlin
  val f: Expression = ...
  val f1 = E `^` f // E === kotlin.math.E
  ```

  但应**特别注意**，`f ^ g` 不是初等函数：

  ```kotlin
  val f1 = f `^` g // 除非下列例外情况之一，否则这一行无法通过编译
  // - f 和 g 中至少包含一个常量表达式，此时是幂函数或指数函数
  // - f 和 g 中有且只有一个是非表达式的数值对象
  // - f 是指数函数，因为 (a^x)^y = a^(x y)，这是初等函数
  ```

  如果有必要定义没有任何未知数存在的表达式（常数表达式），可使用常数表达式：

  ```kotlin
  // Constant(Double) 将一个有理数转换为常数表达式
  val c1 = Constant(1.0 + 2 + 3)
  
  // 这样也是正确的，因为常数表达式也是表达式成分，c2 会被推断为表达式
  val c2 = Constant(1.0) + 2 + 3
  ```

* 微分

  实际上微分也是一种表达式运算，将一个表达式转化为其微分式的表达式：

  ```kotlin
  val x by variable
  val y by variable
  val f = (sqrt(x * x + y) + 1) `^` 2
  val df = d(f) 
  println(df) // 打印：2 x dx + 2 (x^2 + y)^-0.5 x dx + dy + (x^2 + y)^-0.5 dy
  
  ...
  ```

  `dx`、`dy` 是所谓微元的东西，作为一种可乘除相消的因子参与运算。

  微分运算通过和式、积式和复合函数求导的链式法则排出函数部分，保留变量的微元。

  若 `$u`、`$v` 为两个不同的变量，定义 `d d $u ≡ d$u / d$v ≡ d$v / d$u ≡ Constant(.0)`。

  因此，`d(f)/d(x)` 就是 `f(x,y)` 对 `x` 的偏导数：

  ```kotlin
  ...
  
  val dfx = df / d(x)
  println(dfx) // 打印：2 x + 2 (x^2 + y)^-0.5 x
  
  ...
  ```

  可以保存多重微分式，降低求高阶导数的开销：

  ```kotlin
  ...
  
  val ddf = d(df) // 这里实际上完成了全部的微分运算，所谓“求偏导”只是微分项的指数加减法
  val dx = d(x)
  val dy = d(y)
  
  println("∂2f / ∂x2  = ${ddf / (dx * dx)}")
  println("∂2f / ∂x∂y = ${ddf / (dx * dy)}")
  println("∂2f / ∂y2  = ${ddf / (dy * dy)}")
  
  ...
  ```

  ```bash
  ∂2f / ∂x2  = 2 (x^2 + y)^-0.5 - 2 (x^2 + y)^-1.5 x^2 + 2
  ∂2f / ∂x∂y = -2 (x^2 + y)^-1.5 x
  ∂2f / ∂y2  = -0.5 (x^2 + y)^-1.5
  ```

* 代入

  代入是化简、消元一类操作最常见的形式。

  ```kotlin
  val x by variable
  val y by variable
  
  val f = x `^` 2
  println(f.substitute(x, 2)) // 打印：4
  println(f.substitute { this[x] = x * y }) // 打印：x^2 y^2
  ```

  下列代换都受到支持：

  - 求值：把变量代换为常量
  - 复合展开：把变量代换为表达式
  - 换元：把表达式带换成变量

  > 但是暂时还无法实现对和式和积式的部分代换：例如从 `4 x + 4 y` 中代换掉 `x + y` 或从 `x y z` 中代换掉 `x y`。