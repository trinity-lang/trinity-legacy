package com.github.chrisblutz.trinity.lang.types.nativeutils;

import com.github.chrisblutz.trinity.lang.TYObject;
import com.github.chrisblutz.trinity.lang.scope.TYRuntime;
import com.github.chrisblutz.trinity.lang.types.bool.TYBoolean;
import com.github.chrisblutz.trinity.lang.types.maps.TYMap;
import com.github.chrisblutz.trinity.natives.NativeStorage;
import com.github.chrisblutz.trinity.natives.TrinityNatives;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Christopher Lutz
 */
class NativeMap {
    
    static void register() {
        
        TrinityNatives.registerMethod("Map", "initialize", false, null, null, null, (runtime, thisObj, params) -> new TYMap(new HashMap<>()));
        TrinityNatives.registerMethod("Map", "length", false, null, null, null, (runtime, thisObj, params) -> NativeStorage.getMapLength(TrinityNatives.cast(TYMap.class, thisObj)));
        TrinityNatives.registerMethod("Map", "keys", false, null, null, null, (runtime, thisObj, params) -> NativeStorage.getMapKeySet(TrinityNatives.cast(TYMap.class, thisObj)));
        TrinityNatives.registerMethod("Map", "values", false, null, null, null, (runtime, thisObj, params) -> NativeStorage.getMapValues(TrinityNatives.cast(TYMap.class, thisObj)));
        TrinityNatives.registerMethod("Map", "put", false, new String[]{"key", "value"}, null, null, (runtime, thisObj, params) -> {
            
            Map<TYObject, TYObject> map = TrinityNatives.cast(TYMap.class, thisObj).getInternalMap();
            map.put(runtime.getVariable("key"), runtime.getVariable("value"));
            
            NativeStorage.clearMapData(TrinityNatives.cast(TYMap.class, thisObj));
            
            return TYObject.NONE;
        });
        TrinityNatives.registerMethod("Map", "remove", false, new String[]{"key"}, null, null, (runtime, thisObj, params) -> {
            
            NativeStorage.clearMapData(TrinityNatives.cast(TYMap.class, thisObj));
            
            return remove(TrinityNatives.cast(TYMap.class, thisObj), runtime.getVariable("key"), runtime);
        });
        TrinityNatives.registerMethod("Map", "[]", false, new String[]{"key"}, null, null, (runtime, thisObj, params) -> get(TrinityNatives.cast(TYMap.class, thisObj), runtime.getVariable("key"), runtime));
    }
    
    private static TYObject get(TYMap tyMap, TYObject obj, TYRuntime runtime) {
        
        Map<TYObject, TYObject> map = tyMap.getInternalMap();
        
        for (TYObject key : map.keySet()) {
            
            TYBoolean equal = TrinityNatives.cast(TYBoolean.class, key.tyInvoke("==", runtime, null, null, obj));
            
            if (equal.getInternalBoolean()) {
                
                return map.get(key);
            }
        }
        
        return TYObject.NIL;
    }
    
    private static TYObject remove(TYMap tyMap, TYObject obj, TYRuntime runtime) {
        
        Map<TYObject, TYObject> map = tyMap.getInternalMap();
        
        for (TYObject key : map.keySet()) {
            
            TYBoolean equal = TrinityNatives.cast(TYBoolean.class, key.tyInvoke("==", runtime, null, null, obj));
            
            if (equal.getInternalBoolean()) {
                
                return map.remove(key);
            }
        }
        
        return TYObject.NIL;
    }
}
