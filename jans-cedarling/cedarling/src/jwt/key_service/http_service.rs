use super::traits::HttpGet;
use reqwest::blocking::get;

pub struct HttpService;

impl HttpGet for HttpService {
    fn get(&self, uri_str: &str) -> Result<bytes::Bytes, super::Error> {
        let resp = get(uri_str)?;
        Ok(resp.bytes()?)
    }
}
