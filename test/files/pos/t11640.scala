// f-bounded type alias should not result in illegal cycle error
// the problem was that isError calls typeSymbol which eta-expands X while we're completing the sig for `type X`
class C { type X[A <: X[A]] = String }
