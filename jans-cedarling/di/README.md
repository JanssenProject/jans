# DI fork from [link](https://github.com/teloxide/dptree/blob/master/src/di.rs)

Simple and powerfull dependency manager container.

The idea is simple we put type to the container and we can get value with this type.

If type is not in the container it panic

## Example of usage

```rust
let mut map = DependencyMap::new();
map.insert(42i32);
map.insert("hello world");
map.insert_container(deps![true]);

assert_eq!(map.get(), Arc::new(42i32));
assert_eq!(map.get(), Arc::new("hello world"));
assert_eq!(map.get(), Arc::new(true));

container.insert(app_types::PdpID::new());
let pdp_id: Arc<app_types::PdpID> = container.get();

// or we can dereference in this way if type support `Copy`
let pdp_id: app_types::PdpID = *container.get();

```
