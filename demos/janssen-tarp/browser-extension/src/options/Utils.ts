export default class Utils {
    static async generateRandomChallengePair() {
        const secret = await Utils.generateRandomString();
        const encryt = await Utils.sha256(secret);
        const hashed = Utils.base64URLEncode(encryt);
        return { secret, hashed };
    }

    static base64URLEncode(a: ArrayBuffer): string {
        let str = "";
        const bytes = new Uint8Array(a);
        const len = bytes.byteLength;
        for (let i = 0; i < len; i++) {
            str += String.fromCharCode(bytes[i]);
        }
        return btoa(str)
            .replace(/\+/g, "-")
            .replace(/\//g, "_")
            .replace(/=+$/, "");
    }

    static dec2hex(dec: number): string {
        const hex = '0' + dec.toString(16);
        return hex.slice(-2);
    }

    static generateRandomString(): string {
        const array = new Uint32Array(56 / 2);
        window.crypto.getRandomValues(array);
        return Array.from(array, Utils.dec2hex).join('');
    }

    static async sha256(plain: string): Promise<ArrayBuffer> {
        const encoder = new TextEncoder();
        const data = encoder.encode(plain);
        return window.crypto.subtle.digest('SHA-256', data);
    }

    static isJSON(str: string): boolean {
        try {
            JSON.parse(str);
            return true;
        } catch (e) {
            return false;
        }
    }

    static isEmpty(value: unknown): boolean {
        if (value == null) return true;

        if (typeof value === 'string' || Array.isArray(value)) {
            return value.length === 0;
        }

        if (typeof value === 'object') {
            return Object.keys(value as object).length === 0;
        }

        return false;
    }
}
