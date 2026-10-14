// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/** Maps the generated UniFFI errors to the codes of this API. */
final class Errors {

    private Errors() {
    }

    /**
     * Every variant of a generated error is named after its core code ({@code InvalidDigest} for
     * {@code INVALID_DIGEST}), so the class name is the code.
     */
    static ErrorCode code(Exception variant) {
        String name = variant.getClass().getSimpleName();
        StringBuilder code = new StringBuilder(name.length() + 8);
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch) && i > 0) {
                code.append('_');
            }
            code.append(Character.toUpperCase(ch));
        }
        try {
            return ErrorCode.valueOf(code.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("binding error " + name + " has no TRACE error code", e);
        }
    }
}
