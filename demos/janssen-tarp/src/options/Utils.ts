export default class Utils {
    static doSomething(val: string) { return val; }
    static doSomethingElse(val: string) { return val; }

    static async generateRandomChallengePair() {
        const secret = await Utils.generateRandomString();
        const encryt = await Utils.sha256(secret);
        const hashed = Utils.base64URLEncode(encryt);
        return { secret, hashed };
    }

    static base64URLEncode(a) {
        var str = "";
        var bytes = new Uint8Array(a);
        var len = bytes.byteLength;
        for (var i = 0; i < len; i++) {
            str += String.fromCharCode(bytes[i]);
        }

        return btoa(str)
            .replace(/\+/g, "-")
            .replace(/\//g, "_")
            .replace(/=+$/, "");

    }

    static dec2hex(dec) {
        return ('0' + dec.toString(16)).substr(-2)
    }

    static generateRandomString() {
        var array = new Uint32Array(56 / 2);
        window.crypto.getRandomValues(array);
        return Array.from(array, Utils.dec2hex).join('');
    }

    static async sha256(plain) { // returns promise ArrayBuffer
        const encoder = new TextEncoder();
        const data = await encoder.encode(plain);
        return window.crypto.subtle.digest('SHA-256', data);
    }

    static isJSON(str) {
        try {
            JSON.parse(str);
            return true;
        } catch (e) {
            return false;
        }
    }
    
    static isEmpty(value) {
        return (value == null || value.length === 0);
    }
}