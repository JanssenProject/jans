#![feature(rustc_private)]
#![warn(unused_extern_crates)]

extern crate rustc_hir;

use clippy_utils::{diagnostics::span_lint_and_help, sym};
use rustc_hir::{Expr, ExprKind, QPath, TyKind};
use rustc_lint::{LateContext, LateLintPass};

dylint_linting::declare_late_lint! {
    /// ### What it does
    /// Checks for `EntityUid::from_str(&format!(...))` pattern which is explicitly
    /// warned against in the Cedar documentation for performance reasons.
    ///
    /// ### Why is this bad?
    /// The Cedar create documentation specifically warns that using `format!()`
    /// with `EntityUid::from_str` is inefficient. Use string literals instead.
    ///
    /// ### Example
    ///
    /// ```rust
    /// EntityUid::from_str(&format!("{}::\"{}\"", entity_type, id))
    /// ```
    ///
    /// Use instead:
    ///
    /// ```rust
    /// let entity_type_name = EntityTypeName::from_str(entity_type).unwrap();
    /// let entity_id = EntityId::from_str(id).unwrap_or_else(|e| match e {});
    ///
    /// EntityUid::from_type_name_and_id(entity_type_name, entity_id)
    /// ```
    pub BAD_STRING_CONCATENATION,
    Warn,
    "using EntityUid::from_str with format!() is inefficient"
}

impl<'tcx> LateLintPass<'tcx> for BadStringConcatenation {
    fn check_expr(&mut self, cx: &LateContext<'tcx>, expr: &'tcx Expr<'tcx>) {
        if let ExprKind::Call(callee, [arg]) = &expr.kind
            && let ExprKind::Path(QPath::TypeRelative(ty, segment)) = &callee.kind
            && let TyKind::Path(QPath::Resolved(_, path)) = &ty.kind
            && path.segments[0].ident.name.as_str() == "EntityUid"
            && segment.ident.name == sym::from_str
            && let ExprKind::AddrOf(_, _, exp) = arg.kind
            && let ExprKind::Call(_, [format_call]) = exp.kind
            && let ExprKind::Block(block, _) = format_call.kind
            && is_format_macro(cx, block.expr)
        {
            span_lint_and_help(
                cx,
                BAD_STRING_CONCATENATION,
                expr.span,
                "using EntityUid::from_str with format!() is inefficient",
                None,
                "use string literals instead",
            );
        }
    }
}

fn is_format_macro<'tcx>(cx: &LateContext<'tcx>, expr: Option<&Expr<'tcx>>) -> bool {
    if let Some(expr) = expr
        && let ExprKind::Call(callee, _) = &expr.kind
        && let ExprKind::Path(QPath::Resolved(_, path)) = &callee.kind
    {
        if path.segments.len() >= 2 {
            let last_two: Vec<_> = path
                .segments
                .iter()
                .rev()
                .take(2)
                .map(|s| s.ident.name.as_str())
                .collect();

            if last_two.len() == 2 && last_two[0] == "format" && last_two[1] == "fmt" {
                return true;
            }
        }

        // Fallback: check def_id
        if let Some(def_id) = path.res.opt_def_id() {
            let path_str = cx.tcx.def_path_str(def_id);
            return path_str == "alloc::fmt::format";
        }
    }
    false
}

#[test]
fn ui() {
    dylint_testing::ui_test(env!("CARGO_PKG_NAME"), "ui");
}
