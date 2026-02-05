// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#![feature(rustc_private)]
#![warn(unused_extern_crates)]

extern crate rustc_ast;
extern crate rustc_errors;
extern crate rustc_hir;
extern crate rustc_span;

use clippy_utils::{
    diagnostics::{span_lint_and_help, span_lint_and_sugg},
    sym,
};
use rustc_ast::LitKind::Str;
use rustc_errors::Applicability;
use rustc_hir::{Expr, ExprKind, QPath, TyKind};
use rustc_lint::{LateContext, LateLintPass};
use rustc_span::{Symbol, source_map::Spanned};

dylint_linting::declare_late_lint! {
    /// ### What it does
    /// Checks for `EntityUid::from_str(&format!(...))` pattern which is explicitly
    /// warned against in the Cedar documentation for performance reasons.
    ///
    /// ### Why is this bad?
    /// The Cedar crate documentation specifically warns that using `format!()`
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
        // This checks if the expression kind is a function call extracting
        // the function itself and its arguments
        if let ExprKind::Call(callee, [arg]) = &expr.kind
            // Checks the function's kind is a path to definition e.g., Vec::new or EntityUid::from_str
            && let ExprKind::Path(QPath::TypeRelative(ty, segment)) = &callee.kind
            // Checks the function's type is a path to a type e.g., Vec<T> or EntityUid
            && let TyKind::Path(QPath::Resolved(_, path)) = &ty.kind
            // Checks for the type path segments identifier is `EntityUid` or not
            // e.g., cedar_policy::EntityUid::from_str will have 2 path segments cedar_policy and EntityUid
            && path.segments.last().is_some_and(|s| s.ident.name.as_str().eq("EntityUid"))
            // Checks if the function name is `from_str` or not
            && segment.ident.name == sym::from_str
            // Checks if the argument of `from_str` is a reference and extract the expression inside
            && let ExprKind::AddrOf(_, _, exp) = arg.kind
            // Checks if the argument inside `from_str` is a function call then extarct the
            // argument of said function
            && let ExprKind::Call(_, [format_call]) = exp.kind
            && let ExprKind::Block(block, _) = format_call.kind
            && is_format_macro(cx, block.expr)
            && let Some(bexpr) = block.expr
            && let ExprKind::Call(_, [format_arg]) = &bexpr.kind
        {
            if let Some(arg) = extract_string_literal(format_arg) {
                span_lint_and_sugg(
                    cx,
                    BAD_STRING_CONCATENATION,
                    expr.span,
                    "using `EntityUid::from_str` with format! is inefficient",
                    "try this instead",
                    format!("EntityUid::from_str({arg:?})"),
                    Applicability::MachineApplicable,
                );
            } else {
                span_lint_and_help(
                    cx,
                    BAD_STRING_CONCATENATION,
                    expr.span,
                    "using `EntityUid::from_str` with format! is inefficient",
                    None,
                    "consider using `EntityUid::from_type_name_and_id` instead",
                );
            }
        }
    }
}

/// Checks if the given expression is a format macro or not
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

            if last_two[0] == "format" && last_two[1] == "fmt" {
                return true;
            }
        }

        // Fallback: check def_id
        if let Some(def_id) = path.res.opt_def_id() {
            let path_str = cx.tcx.def_path_str(def_id);
            return matches!(path_str.as_str(), "alloc::fmt::format" | "std::fmt::format");
        }
    }
    false
}

// Checks if the argument of format macro is a string literal and extract it
fn extract_string_literal(expr: &Expr<'_>) -> Option<Symbol> {
    if let ExprKind::Call(_, [arg]) = expr.kind
        && let ExprKind::AddrOf(_, _, expr) = arg.kind
        && let ExprKind::Array([element]) = expr.kind
        && let ExprKind::Lit(Spanned {
            node: Str(item, _), ..
        }) = element.kind
    {
        return Some(item);
    }

    None
}

#[test]
fn ui() {
    dylint_testing::ui_test(env!("CARGO_PKG_NAME"), "ui");
}
