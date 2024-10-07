/*
 * This software is available under the MIT License
 * See https://github.com/teloxide/dptree/blob/master/LICENSE for full text.
 *
 * Copyright (c) 2021-2022 teloxide
 */

//! An implementation of [dependency injection].
//!
//! If you do not know what is dependency injection (DI), please read [this
//! discussion on StackOverflow], then come back. The only difference is that in
//! `dptree`, we inject objects into function-handlers, not into objects.
//!
//! Currently, the only container is [`DependencyMap`]. It implements the DI
//! pattern completely, but be careful: it can panic when you do not provide
//! necessary types. See more in its documentation.
//!
//! [dependency injection]: https://en.wikipedia.org/wiki/Dependency_injection
//! [this discussion on StackOverflow]: https://stackoverflow.com/questions/130794/what-is-dependency-injection

use std::{
    any::{Any, TypeId},
    collections::HashMap,
    fmt::{Debug, Formatter, Write},
    ops::Deref,
    sync::Arc,
};

/// A DI container from which we can extract a value of a given type.
///
/// There are two possible ways to handle the situation when your container
/// cannot return a value of specified type:
///
/// 1. Do not implement [`DependencySupplier`] for the type. It often requires some type-level manipulations.
/// 2. Runtime panic. Be careful in this case: check whether you add your type to the container.
///
/// A concrete solution is left to a particular implementation.
pub trait DependencySupplier<Value> {
    /// Get the value.
    ///
    /// We assume that all values are stored in `Arc<_>`.
    fn get(&self) -> Arc<Value>;
}

/// A DI container with multiple dependencies.
///
/// This DI container stores types by their corresponding type identifiers. It
/// cannot prove at compile-time that a type of a requested value exists within
/// the container, so if you do not provide necessary types but they were
/// requested, it will panic.
///
/// # Examples
///
/// ```
/// # use std::sync::Arc;
/// use di::{DependencyMap, DependencySupplier};
///
/// let mut container = DependencyMap::new();
/// container.insert(5_i32);
/// container.insert("abc");
///
/// assert_eq!(container.get(), Arc::new(5_i32));
/// assert_eq!(container.get(), Arc::new("abc"));
///
/// // If a type of a value already exists within the container, it will be replaced.
/// let old_value = container.insert(10_i32).unwrap();
///
/// assert_eq!(old_value, Arc::new(5_i32));
/// assert_eq!(container.get(), Arc::new(10_i32));
/// ```
///
/// When a value is not found within the container, it will panic:
///
/// ```should_panic
/// # use std::sync::Arc;
/// use di::{DependencyMap, DependencySupplier};
/// let mut container = DependencyMap::new();
/// container.insert(10i32);
/// container.insert(true);
/// container.insert("static str");
///
/// // thread 'main' panicked at 'alloc::string::String was requested, but not provided. Available types:
/// //    &str
/// //    bool
/// //    i32
/// // ', /media/hirrolot/772CF8924BEBB279/Documents/Rust/dptree/src/di.rs:150:17
/// // note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
/// let string: Arc<String> = container.get();
/// ```
#[derive(Default, Clone)]
pub struct DependencyMap {
    map: HashMap<TypeId, Dependency>,
}

#[derive(Clone)]
struct Dependency {
    type_name: &'static str,
    inner: Arc<dyn Any + Send + Sync>,
}

impl PartialEq for DependencyMap {
    fn eq(&self, other: &Self) -> bool {
        let keys1 = self.map.keys();
        let keys2 = other.map.keys();
        keys1.zip(keys2).map(|(k1, k2)| k1 == k2).all(|x| x)
    }
}

impl DependencyMap {
    pub fn new() -> Self {
        Self::default()
    }

    /// Inserts a value into the container.
    ///
    /// If the container do not has this type present, `None` is returned.
    /// Otherwise, the value is updated, and the old value is returned.
    pub fn insert<T: Send + Sync + 'static>(&mut self, item: T) -> Option<Arc<T>> {
        self.map
            .insert(
                TypeId::of::<T>(),
                Dependency {
                    type_name: std::any::type_name::<T>(),
                    inner: Arc::new(item),
                },
            )
            .map(|dep| dep.inner.downcast().expect("Values are stored by TypeId"))
    }

    /// Inserts all dependencies from another container into itself.
    pub fn insert_container(&mut self, container: Self) {
        self.map.extend(container.map);
    }

    /// Removes a value from the container.
    ///
    /// If the container do not has this type present, `None` is returned.
    /// Otherwise, the value is removed and returned.
    pub fn remove<T: Send + Sync + 'static>(&mut self) -> Option<Arc<T>> {
        self.map
            .remove(&TypeId::of::<T>())
            .map(|dep| dep.inner.downcast().expect("Values are stored by TypeId"))
    }

    fn available_types(&self) -> String {
        let mut list = String::new();

        for dep in self.map.values() {
            writeln!(list, "    {}", dep.type_name).unwrap();
        }

        list
    }
}

impl Debug for DependencyMap {
    fn fmt(&self, f: &mut Formatter<'_>) -> Result<(), std::fmt::Error> {
        f.debug_struct("DependencyMap").finish()
    }
}

impl<V> DependencySupplier<V> for DependencyMap
where
    V: Send + Sync + 'static,
{
    fn get(&self) -> Arc<V> {
        self.map
            .get(&TypeId::of::<V>())
            .unwrap_or_else(|| {
                panic!(
                    "{} was requested, but not provided. Available types:\n{}",
                    std::any::type_name::<V>(),
                    self.available_types()
                )
            })
            .clone()
            .inner
            .downcast::<V>()
            .expect("Checked by .unwrap_or_else()")
    }
}

impl<V, S> DependencySupplier<V> for Arc<S>
where
    S: DependencySupplier<V>,
{
    fn get(&self) -> Arc<V> {
        self.deref().get()
    }
}

/// Constructs [`DependencyMap`] with a list of dependencies.
///
/// # Examples
///
/// ```
/// use di::{DependencyMap, DependencySupplier, deps};
///
/// let map = deps![123, "abc", true];
///
/// let i: i32 = *map.get();
/// let str: &str = *map.get();
/// let b: bool = *map.get();
///
/// assert!(i == 123);
/// assert!(str == "abc");
/// assert!(b == true);
/// ```
#[macro_export]
macro_rules! deps {
    ($($dep:expr),*) => {
        {
            // In the case if this macro receives zero arguments.
            #[allow(unused_mut)]
            let mut map = DependencyMap::new();
            $(map.insert($dep);)*
            map
        }
    }
}

/// Insert some value to a container.
pub trait Insert<Value> {
    /// Inserts `value` into itself, returning the previous value, if exists.
    fn insert(&mut self, value: Value) -> Option<Arc<Value>>;
}

impl<T: Send + Sync + 'static> Insert<T> for DependencyMap {
    fn insert(&mut self, value: T) -> Option<Arc<T>> {
        DependencyMap::insert(self, value)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn get() {
        let mut map = DependencyMap::new();
        map.insert(42i32);
        map.insert("hello world");
        map.insert_container(deps![true]);

        assert_eq!(map.get(), Arc::new(42i32));
        assert_eq!(map.get(), Arc::new("hello world"));
        assert_eq!(map.get(), Arc::new(true));
    }
}
