// Copyright 2024 LY Corporation
//
// LY Corporation licenses this file to you under the Apache License,
// version 2.0 (the "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at:
//
//   https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.

/// It defines the behavior of a credential source storage for handling a public
/// key credential source and its signature counter.
///
/// You need to store the following data contained in PublicKeyCredentialSource
/// and a signature counter for each credential source at a minimum.
public protocol CredentialSourceStorage {
    func load(_ credId: String) -> Result<PublicKeyCredentialSource?, Error>
    func loadAll() -> Result<[PublicKeyCredentialSource]?, Error>
    func store(_ credSrc: PublicKeyCredentialSource) -> Result<(), Error>
    func delete(_ credId: String) -> Result<(), Error>
    func increaseSignatureCounter(_ credId: String) -> Result<UInt32, Error>
}
