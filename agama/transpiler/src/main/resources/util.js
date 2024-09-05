let 
    _primitiveUtils = Packages.io.jans.agama.engine.misc.PrimitiveUtils,
    _scriptUtils = Packages.io.jans.agama.engine.script.ScriptUtils,
    _logUtils = Packages.io.jans.agama.engine.script.LogUtils,
    _flowCrashEx = Packages.io.jans.agama.engine.exception.FlowCrashException,
    _errorUtil = Packages.org.mozilla.javascript.NativeErrorUtil,
    _booleanCls = Packages.java.lang.Class.forName("java.lang.Boolean"),
    _numberCls = Packages.java.lang.Class.forName("java.lang.Number"),
    _integerCls = Packages.java.lang.Class.forName("java.lang.Integer"),
    _stringCls = Packages.java.lang.Class.forName("java.lang.String"),
    _collectionCls = Packages.java.lang.Class.forName("java.util.Collection"),
    _listCls = Packages.java.lang.Class.forName("java.util.List"),
    _mapCls = Packages.java.lang.Class.forName("java.util.Map"),
    _exceptionCls = Packages.java.lang.Class.forName("java.lang.Exception")

function _renderReplyFetch(base, page, allowCallbackResume, data) {
    if (_isObject(data))
        return _scriptUtils.pauseForRender(base + "/" + page, allowCallbackResume, data)
    throw new TypeError("Data passed to RRF was not a map or Java equivalent")
}

function _redirectFetchAtCallback(url) {
    if (_isString(url)) {
        let jsonStr = _scriptUtils.pauseForExternalRedirect(url).second
        //string parsing via JS was preferred over returning a Java Map directly
        //because it feels more natural for RRF to return a native JS object;
        //it creates the illusion there was no Java involved
        return JSON.parse(jsonStr)
    }
    throw new TypeError("Data passed to RFAC was not a string")
}

function _log2(qname, args) {
    _logUtils.logPrefixed(qname, args.map(_scan))
}

function _log(args) {   //Preserved for backwards compat (jans <= 1.0.15)
    _logUtils.log(args.map(_scan))
}

function _equals(a, b) {
    return _scriptUtils.testEquality(_scan(a), _scan(b))
}

function _actionCall(instance, clsName, method, args) {
    return _scriptUtils.callAction(_scan(instance), clsName, method, args.map(_scan))
}

function _flowCall(flowName, basePath, urlOverrides, args) {

    if (!_isString(flowName))
        return _flowCallErr(new TypeError("Flow name is not a string"))

    let mapping = _scriptUtils.templatesMapping(basePath, urlOverrides)
    let p = _scriptUtils.prepareSubflow(flowName, mapping)
    let params = args.map(_scan)
    params.splice(0, 0, p.second)

    let f = p.first
    //Modify p to avoid serializing a Java Pair in a RRF call (inside function f)
    //wrapping with ArrayList is a workaround for a kryo deserialization exception
    p = new Packages.java.util.ArrayList(mapping.values())
    mapping = null

    let result = f.apply(null, params)
    _scriptUtils.closeSubflow()

    if (_isNil(result))
        return _flowCallErr(new Error(
                "No Finish instruction was reached during execution of " + flowName))
    
    return { value: result,
        //determines if the parent should handle this returned value
        bubbleUp: result.aborted == true && !_scriptUtils.pathMatching(result.url, p) }

}

function _flowCallErr(e) {
    return { value: _makeJavaException(null, e), bubbleUp: false }
}

function _makeJavaException(qname, e) {

    let msg = e.toString()
    _log2(qname, ["@e An error occurred:", msg])
    _log2(qname, ["@t \n", _errorUtil.stackTraceOf(e)])
    return new _flowCrashEx(msg)

}

function _finish(val) {

    let javaish = _javaish(val)
    
    if (_isString(val, javaish))
        return { success: true, data: { userId: val } }
    else if (_isBool(val, javaish))
        return { success: val }
    else if (_isObject(val, javaish) && _isBool(val.success))
        return val

    throw new Error("Cannot determine whether Finish value should be successful or not")

}

function _abort(url, data) {
    return { aborted: true, url: url, data: data }
}

function _scan(val) {
    //treat undefined, null, or a Java method reference as null
    if (_isNil(val) || typeof val === "function") return null    
    return val
}

function _ensureNumber(val, msg) {
    if (!_isNumber(val)) throw new TypeError(msg)
}

function _iterable(val, msg) {
    if (_isMap(val)) return Object.keys(val)
    if (_isList(val) || _isString(val)) return val
    throw new TypeError(msg)
}

//Ensures val is a string and returns it
//(short function name used so generated code is compact)
function _sc(val, symbol) {
    if (_isString(val)) return val
    throw new TypeError(symbol + " is not a string")
}

//Ensures val is an integer (greater than or equal to 0) and returns it
//(short function name used so generated code is compact)
function _ic(val, symbol) {

    if (_javaish(val)) {
        if (_integerCls.isInstance(val) && val >= 0)
            return val
    } else if (Number.isInteger(val) && val >= 0)
        return val

    throw new TypeError(symbol + " is not zero or a positive integer")

}

function _isJavaException(val) {
    return _exceptionCls.isInstance(val)
}

function _isObject(val, javaish) {

    let jish = _isNil(javaish) ? _javaish(val) : javaish
    if (jish) {
        let cls = val.getClass()
        return !(_stringCls.isInstance(val) || _primitiveUtils.isPrimitive(cls, true)
            || cls.isArray() || _collectionCls.isInstance(val))
    }
    return !_isNil(val) && !Array.isArray(val) && typeof val === "object"

}

function _isMap(val, javaish) {
    let jish = _isNil(javaish) ? _javaish(val) : javaish
    return jish ? _mapCls.isInstance(val) : _isObject(val, false)
}

function _isList(val, javaish) {

    let jish = _isNil(javaish) ? _javaish(val) : javaish
    if (jish) {
        let cls = val.getClass()
        return cls.isArray() || _listCls.isAssignableFrom(cls)
    }
    return Array.isArray(val)

}

function _isBool(val, javaish) {
    let jish = _isNil(javaish) ? _javaish(val) : javaish
    return jish ? _booleanCls.isInstance(val) : typeof val === "boolean"
}

function _isString(val, javaish) {
    let jish = _isNil(javaish) ? _javaish(val) : javaish
    return jish ? _stringCls.isInstance(val) : typeof val === "string"
}

function _isNumber(val, javaish) {

    let jish = _isNil(javaish) ? _javaish(val) : javaish
    if (jish)
        //Only Double/Float/Long/Integer/Short/Byte objects 
        return _numberCls.isInstance(val) && _primitiveUtils.isPrimitive(val.getClass(), true) 
    return typeof val === "number" && !isNaN(val)

}

function _isNil(val) {
    //undefined and null are treated the same way here... 
    return val == null
}

function _javaish(val) {

    try {
        val.getClass()
        //Instances of org.mozilla.javascript.NativeArray/NativeObject throw TypeError in the above line
        //as well as native boolean/string/array/object defined in Javascript code
        //plus null
        return true
    } catch (e) {
        return false
    }

}
