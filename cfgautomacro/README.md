# A configurable auto-macro

**Requires a pending change in KeY.** 

Before it gets lost: A macro, which uses a configurable strategy.

1. Defines `AdaptableStrategy` which is a proxy for a strategy `cost` and can be configured by
   two integer maps `f`/`s`. It calculates the costs by `cost'(x) = f(x) * cost(x) + s(x)`.
2. `ConfigurableMacro -- A macro that replaces the current strategy
3. `AutoConfigurableMacro` -- A macro used in the GUI. It shows a dialog for manipulated `f(x)` and `s(x)`.
   More a gimmick than useful.

![user interface](share/ui.png)