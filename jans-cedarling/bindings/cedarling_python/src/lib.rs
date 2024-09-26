use pyo3::prelude::*;
use pyo3::Bound;

#[pymodule]
fn cedarling_python(_m: &Bound<'_, PyModule>) -> PyResult<()> {
    Ok(())
}
