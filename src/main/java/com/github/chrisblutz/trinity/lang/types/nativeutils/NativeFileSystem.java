package com.github.chrisblutz.trinity.lang.types.nativeutils;

import com.github.chrisblutz.trinity.files.FilePrivilege;
import com.github.chrisblutz.trinity.files.FileUtils;
import com.github.chrisblutz.trinity.lang.TYObject;
import com.github.chrisblutz.trinity.lang.errors.Errors;
import com.github.chrisblutz.trinity.lang.types.bool.TYBoolean;
import com.github.chrisblutz.trinity.lang.types.strings.TYString;
import com.github.chrisblutz.trinity.natives.TrinityNatives;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Christopher Lutz
 */
class NativeFileSystem {
    
    static void register() {
        
        TrinityNatives.registerMethod(TrinityNatives.Classes.FILE_SYSTEM, "normalize", (runtime, thisObj, params) -> {
            
            TYObject object = runtime.getVariable("path");
            
            return new TYString(new File(TrinityNatives.cast(TYString.class, object).getInternalString()).getAbsolutePath());
        });
        TrinityNatives.registerMethod(TrinityNatives.Classes.FILE_SYSTEM, "create", (runtime, thisObj, params) -> {
            
            TYObject object = runtime.getVariable("path");
            
            try {
                
                File f = new File(TrinityNatives.cast(TYString.class, object).getInternalString());
                if (f.getParentFile() != null && !f.getParentFile().exists()) {
                    
                    if (f.getParentFile().mkdirs()) {
                        
                        Errors.throwError(Errors.Classes.IO_ERROR, runtime, "Unable to create parent directories.");
                    }
                }
                return TYBoolean.valueFor(f.createNewFile());
                
            } catch (Exception e) {
                
                Errors.throwError(Errors.Classes.IO_ERROR, runtime, "An error occurred creating a file at '" + ((TYString) object).getInternalString() + "'.");
            }
            
            return TYBoolean.FALSE;
        });
        TrinityNatives.registerMethod(TrinityNatives.Classes.FILE_SYSTEM, "open", (runtime, thisObj, params) -> {
            
            TYObject path = runtime.getVariable("path");
            TYObject privilege = runtime.getVariable("privileges");
            TYObject append = runtime.getVariable("append");
            
            List<FilePrivilege> privileges = new ArrayList<>();
            String privilegeStr = TrinityNatives.cast(TYString.class, privilege).getInternalString();
            if (privilegeStr.contains("r")) {
                
                privileges.add(FilePrivilege.READ);
            }
            if (privilegeStr.contains("w")) {
                
                privileges.add(FilePrivilege.WRITE);
            }
            
            FileUtils.open(TrinityNatives.cast(TYString.class, path).getInternalString(), privileges, TrinityNatives.toBoolean(append));
            
            return TYObject.NONE;
        });
        TrinityNatives.registerMethod(TrinityNatives.Classes.FILE_SYSTEM, "read", (runtime, thisObj, params) -> {
            
            TYObject path = runtime.getVariable("path");
            
            return FileUtils.read(TrinityNatives.cast(TYString.class, path).getInternalString());
        });
        TrinityNatives.registerMethod(TrinityNatives.Classes.FILE_SYSTEM, "write", (runtime, thisObj, params) -> {
            
            TYObject path = runtime.getVariable("path");
            TYObject str = runtime.getVariable("str");
            
            FileUtils.write(TrinityNatives.cast(TYString.class, path).getInternalString(), TrinityNatives.toString(str, runtime));
            
            return TYObject.NONE;
        });
        TrinityNatives.registerMethod(TrinityNatives.Classes.FILE_SYSTEM, "close", (runtime, thisObj, params) -> {
            
            TYObject path = runtime.getVariable("path");
            
            FileUtils.close(TrinityNatives.cast(TYString.class, path).getInternalString());
            
            return TYObject.NONE;
        });
    }
}
