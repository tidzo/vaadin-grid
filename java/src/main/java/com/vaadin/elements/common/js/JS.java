package com.vaadin.elements.common.js;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

/**
 * Class with static utilities for @JsType
 *
 * TODO: revisit when JsInterop supports static or default methods in @JsType
 * interfaces
 *
 */
public abstract class JS {

    // This has to match with the @JsNamespace of the package-info of exported
    // components
    public static final String VAADIN_JS_NAMESPACE = "vaadin.elements";

    @SuppressWarnings("unchecked")
    public static <T> T createJsType(Class<T> clz) {
        if (clz == JsArrayMixed.class || clz == JSArray.class) {
            return (T) JavaScriptObject.createArray();
        }
        return (T) JavaScriptObject.createObject();
    }

    @SuppressWarnings("unchecked")
    public static <T> JSArray<T> createArray() {
        return createJsType(JSArray.class);
    }

    /**
     * Box a native JS array in a Java List. It does not have any performance
     * penalty because we directly change the native array of super ArrayList
     * implementation.
     */
    public static native <T> List<T> asList(JavaScriptObject o)
    /*-{
        var l = @java.util.ArrayList::new()();
        l.@java.util.ArrayList::array = o;
        return l;
    }-*/;

    public static native boolean isPrimitiveType(Object dataItem)
    /*-{
        return Object(dataItem) !== dataItem;
    }-*/;

    public static void definePropertyAccessors(Object jso, String propertyName,
            Setter setter, Getter getter) {
        JavaScriptObject setterJSO = setter != null ? wrapFunction((p0, p1, p2) -> {
            // Empty Strings are interpreted as null for some reason
            // so they need some special attention.
            JSONValue jsonValue = new JSONObject((JavaScriptObject) p1)
                    .get("value");
            if (!JS.isUndefinedOrNull(jsonValue)
                    && jsonValue.isString() != null
                    && "".equals(jsonValue.isString().stringValue())) {
                setter.setValue("");
            } else {
                // Otherwise handle normally
                setter.setValue(p0);
            }
            return null;
        })
                : null;

        JavaScriptObject getterJSO = getter != null ? wrapFunction((p0, p1, p2) -> {
            JSArray<Object> array = JS.createArray();
            array.push(getter.getValue());
            return array;
        })
                : null;
        definePropertyAccessors((JavaScriptObject) jso, propertyName,
                setterJSO, getterJSO);
    }

    private static native void definePropertyAccessors(
            JavaScriptObject jsObject, String propertyName,
            JavaScriptObject setter, JavaScriptObject getter)
    /*-{
      var _value = jsObject[propertyName];

      Object.defineProperty(jsObject, propertyName, {
        get: function() {
            if (getter) {
                return getter()[0];
            }
            return _value;
        },
        set: function(value) {
            if (setter){
                setter(value, {value: value});
            }
            _value = value;
        }
      });

      if (_value !== undefined){
          jsObject[propertyName] = _value;
      }
    }-*/;

    public interface Setter {
        void setValue(Object value);
    }

    public interface Getter {
        Object getValue();
    }

    public static <T> T exec(Object o, Object p0) {
        return exec(o, p0, null);
    }

    public static native <T> T exec(Object o, Object p0, Object p1)
    /*-{
      return o(p0, p1);
    }-*/;

    public static native boolean isUndefinedOrNull(Object o)
    /*-{
      return o === undefined || o === null;
    }-*/;

    public static native boolean isObject(Object o)
    /*-{
      return typeof o === "object" && o !== null;
    }-*/;

    public static native JavaScriptObject getError(String msg)
    /*-{
        return new Error(msg || '');
    }-*/;

    public static native JavaScriptObject getUndefined()
    /*-{
        return undefined;
    }-*/;

    public static native boolean isArray(JavaScriptObject o)
    /*-{
    return Array.isArray(o);
    }-*/;

    public static native <T> T prop(JavaScriptObject o, String name)
    /*-{
    return o[name];
    }-*/;

    public static native void prop(JavaScriptObject o, String name, Object value)
    /*-{
    o[name] = value;
    }-*/;

    public static native JavaScriptObject wrapFunction(Function f)
    /*-{
    return function(p0,p1,p2) {
        return f.@com.vaadin.elements.common.js.Function::f(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)(p0,p1,p2);
    }
    }-*/;

    public static <T> T jsni(JavaScriptObject o, String functionName,
            Object... params) {
        Object p0 = params.length > 0 ? params[0] : null;
        Object p1 = params.length > 1 ? params[1] : null;
        Object p2 = params.length > 2 ? params[2] : null;
        return jsni(o, functionName, p0, p1, p2);
    }

    private static native <T> T jsni(JavaScriptObject o, String functionName,
            Object p0, Object p1, Object p2)
    /*-{
     return o[functionName](p0,p1,p2);
    }-*/;
}
