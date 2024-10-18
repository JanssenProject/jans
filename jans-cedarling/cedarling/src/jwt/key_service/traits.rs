use super::error::Error;
use bytes::Bytes;

pub trait HttpGet: Send + Sync {
    fn get(&self, uri_str: &str) -> Result<Bytes, Error>;
}
