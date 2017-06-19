package com.github.chrisblutz.trinity.lang;

import com.github.chrisblutz.trinity.interpreter.Scope;
import com.github.chrisblutz.trinity.lang.errors.Errors;
import com.github.chrisblutz.trinity.lang.procedures.ProcedureAction;
import com.github.chrisblutz.trinity.lang.procedures.TYProcedure;
import com.github.chrisblutz.trinity.lang.scope.TYRuntime;
import com.github.chrisblutz.trinity.natives.NativeStorage;
import com.github.chrisblutz.trinity.plugins.PluginLoader;
import com.github.chrisblutz.trinity.plugins.api.Events;

import java.util.*;


/**
 * @author Christopher Lutz
 */
public class TYClass {
    
    private List<TYClass> classes = new ArrayList<>();
    private String name, shortName;
    private TYMethod constructor;
    private TYClass superclass;
    private String superclassString;
    private String[] importedForSuperclass;
    private TYModule module;
    private List<TYClass> inheritanceTree = new ArrayList<>();
    private Map<String, TYMethod> methods = new HashMap<>();
    private Map<String, TYObject> variables = new HashMap<>();
    private List<ProcedureAction> initializationActions = new ArrayList<>();
    private boolean initialized = false;
    
    private List<String> callableMethods = new ArrayList<>();
    
    private String[] leadingComments = null;
    
    public TYClass(String name, String shortName) {
        
        this(name, shortName, name.contentEquals("Trinity.Object") ? null : ClassRegistry.getClass("Trinity.Object"));
    }
    
    public TYClass(String name, String shortName, TYClass superclass) {
        
        this.name = name;
        this.shortName = shortName;
        this.superclass = superclass;
        
        inheritanceTree = compileInheritanceTree();
        inheritanceTree.add(this);
    }
    
    private List<TYClass> compileInheritanceTree() {
        
        List<TYClass> tree = new ArrayList<>();
        
        if (superclass != null) {
            
            tree.add(superclass);
            
            tree.addAll(superclass.compileInheritanceTree());
        }
        
        return tree;
    }
    
    private Set<String> compileCallableMethods() {
        
        Set<String> methods = new LinkedHashSet<>();
        
        methods.addAll(getMethodNames());
        
        if (superclass != null) {
            
            methods.addAll(superclass.compileCallableMethods());
        }
        
        return methods;
    }
    
    public boolean hasVariable(String name) {
        
        return getVariables().containsKey(name);
    }
    
    public TYObject getVariable(String name) {
        
        return getVariables().getOrDefault(name, TYObject.NIL);
    }
    
    public void setVariable(String name, TYObject object) {
        
        getVariables().put(name, object);
        
        PluginLoader.triggerEvent(Events.CLASS_VARIABLE_UPDATE, this, name, object);
    }
    
    public Map<String, TYObject> getVariables() {
        
        return variables;
    }
    
    public String getName() {
        
        return name;
    }
    
    public String getShortName() {
        
        return shortName;
    }
    
    public TYClass getSuperclass() {
        
        return superclass;
    }
    
    public void setSuperclass(TYClass superclass) {
        
        this.superclass = superclass;
    }
    
    public void setSuperclassString(String string, String[] imports) {
        
        this.superclassString = string;
        this.importedForSuperclass = imports;
    }
    
    public boolean isInstanceOf(TYClass tyClass) {
        
        return inheritanceTree.contains(tyClass);
    }
    
    public void addClass(TYClass tyClass) {
        
        classes.add(tyClass);
    }
    
    public List<TYClass> getClasses() {
        
        return classes;
    }
    
    public boolean hasClass(String shortName) {
        
        for (TYClass tyClass : getClasses()) {
            
            if (tyClass.getShortName().contentEquals(shortName)) {
                
                return true;
            }
        }
        
        return false;
    }
    
    public TYClass getClass(String shortName) {
        
        for (TYClass tyClass : getClasses()) {
            
            if (tyClass.getShortName().contentEquals(shortName)) {
                
                return tyClass;
            }
        }
        
        return null;
    }
    
    public TYModule getModule() {
        
        return module;
    }
    
    public void setModule(TYModule module) {
        
        this.module = module;
    }
    
    public TYObject tyInvoke(String methodName, TYRuntime runtime, TYProcedure procedure, TYRuntime procedureRuntime, TYObject thisObj, TYObject... params) {
        
        return tyInvoke(this, methodName, runtime, procedure, procedureRuntime, thisObj, params);
    }
    
    public TYObject tyInvoke(TYClass originClass, String methodName, TYRuntime runtime, TYProcedure procedure, TYRuntime procedureRuntime, TYObject thisObj, TYObject... params) {
        
        if (methodName.contentEquals("new")) {
            
            if (constructor != null) {
                
                Scope scope = constructor.getScope();
                boolean run = checkScope(scope, constructor, runtime);
                
                if (run) {
                    
                    TYRuntime newRuntime = runtime.clone();
                    newRuntime.clearVariables();
                    
                    TYObject newObj = new TYObject(this);
                    
                    newRuntime.setVariable("this", newObj);
                    newRuntime.setScope(newObj, false);
                    newRuntime.setModule(getModule());
                    newRuntime.setTyClass(this);
                    newRuntime.importModules(constructor.getImportedModules());
                    
                    TYObject obj = constructor.getProcedure().call(newRuntime, procedure, procedureRuntime, newObj, params);
                    
                    if (newRuntime.isReturning()) {
                        
                        Errors.throwError("Trinity.Errors.ReturnError", "Cannot return a value from a constructor.", runtime);
                        
                    } else if (obj.getObjectClass().isInstanceOf(ClassRegistry.getClass("Trinity.Map")) || obj.getObjectClass().isInstanceOf(ClassRegistry.getClass("Trinity.Procedure"))) {
                        
                        newObj = obj;
                    }
                    
                    return newObj;
                    
                } else {
                    
                    Errors.throwError("Trinity.Errors.ScopeError", "Constructor cannot be accessed from this context because it is marked '" + scope.toString() + "'.", runtime);
                    
                    return TYObject.NONE;
                }
                
            } else {
                
                return new TYObject(this);
            }
            
        } else if (methods.containsKey(methodName)) {
            
            TYMethod method = methods.get(methodName);
            
            Scope scope = method.getScope();
            boolean run = checkScope(scope, method, runtime);
            
            if (run) {
                
                TYRuntime newRuntime = runtime.clone();
                newRuntime.setModule(getModule());
                newRuntime.setTyClass(this);
                newRuntime.importModules(method.getImportedModules());
                newRuntime.clearVariables();
                
                if (method.isStaticMethod()) {
                    
                    newRuntime.setScope(NativeStorage.getClassObject(this), true);
                    
                } else {
                    
                    if (thisObj == TYObject.NONE) {
                        
                        Errors.throwError("Trinity.Errors.ScopeError", "Instance method '" + methodName + "' cannot be called from a static context.", runtime);
                    }
                    
                    newRuntime.setVariable("this", thisObj);
                    newRuntime.setScope(thisObj, false);
                }
                
                TYObject result = method.getProcedure().call(newRuntime, procedure, procedureRuntime, thisObj, params);
                
                if (newRuntime.isReturning()) {
                    
                    return newRuntime.getReturnObject();
                }
                
                return result;
                
            } else {
                
                Errors.throwError("Trinity.Errors.ScopeError", "Method '" + methodName + "' cannot be accessed from this context because it is marked '" + scope.toString() + "'.", runtime);
                
                return TYObject.NONE;
            }
            
        } else if (getSuperclass() != null) {
            
            return getSuperclass().tyInvoke(originClass, methodName, runtime, procedure, procedureRuntime, thisObj, params);
            
        } else if (ClassRegistry.getClass("Trinity.Kernel").getMethods().containsKey(methodName)) {
            
            return ClassRegistry.getClass("Trinity.Kernel").tyInvoke(originClass, methodName, runtime, procedure, procedureRuntime, thisObj, params);
            
        } else {
            
            Errors.throwError("Trinity.Errors.MethodNotFoundError", "No method '" + methodName + "' found in '" + originClass.getName() + "'.", runtime);
        }
        
        return TYObject.NONE;
    }
    
    private boolean checkScope(Scope scope, TYMethod method, TYRuntime runtime) {
        
        switch (scope) {
            
            case PUBLIC:
                
                return true;
            
            case MODULE_PROTECTED:
                
                return method.getContainerClass().getModule() == runtime.getModule();
            
            case PROTECTED:
                
                return runtime.getTyClass().isInstanceOf(method.getContainerClass());
            
            case PRIVATE:
                
                return method.getContainerClass() == runtime.getTyClass();
            
            default:
                
                return false;
        }
    }
    
    public void registerMethod(TYMethod method) {
        
        if (methods.containsKey(method.getName()) && methods.get(method.getName()).isSecureMethod()) {
            
            return;
        }
        
        if (method.getName().contentEquals("initialize")) {
            
            constructor = method;
            
            methods.put(method.getName(), method);
            
        } else {
            
            methods.put(method.getName(), method);
            
            if (method.getName().contentEquals("main")) {
                
                ClassRegistry.registerMainClass(this);
            }
        }
        
        PluginLoader.triggerEvent(Events.METHOD_UPDATE, this, method);
    }
    
    public Map<String, TYMethod> getMethods() {
        
        return methods;
    }
    
    public TYMethod[] getMethodArray() {
        
        return methods.values().toArray(new TYMethod[methods.values().size()]);
    }
    
    public TYMethod getMethod(String name) {
        
        return getMethods().getOrDefault(name, null);
    }
    
    public Collection<String> getMethodNames() {
        
        return methods.keySet();
    }
    
    public boolean respondsTo(String method) {
        
        return callableMethods.contains(method);
    }
    
    public String[] getLeadingComments() {
        
        return leadingComments;
    }
    
    public void setLeadingComments(String[] leadingComments) {
        
        this.leadingComments = leadingComments;
    }
    
    public void addInitializationActions(List<ProcedureAction> actions) {
        
        initializationActions.addAll(actions);
    }
    
    public void runInitializationActions(TYRuntime runtime) {
        
        if (!initialized) {
            
            initialized = true;
            
            for (ProcedureAction action : initializationActions) {
                
                action.onAction(runtime, TYObject.NONE);
            }
        }
    }
    
    public void performFinalSetup() {
        
        if (superclassString != null) {
            
            if (module != null && module.hasClass(superclassString)) {
                
                setSuperclass(module.getClass(superclassString));
                
            } else {
                
                boolean found = false;
                
                for (String modStr : importedForSuperclass) {
                    
                    TYModule module = ModuleRegistry.getModule(modStr);
                    
                    if (module.hasClass(superclassString)) {
                        
                        found = true;
                        setSuperclass(module.getClass(superclassString));
                        break;
                    }
                }
                
                if (!found) {
                    
                    if (ClassRegistry.classExists(superclassString)) {
                        
                        setSuperclass(ClassRegistry.getClass(superclassString));
                        
                    } else {
                        
                        Errors.throwError("Trinity.Errors.ClassNotFoundError", "Class " + superclassString + " does not exist.");
                    }
                }
            }
            
            inheritanceTree = compileInheritanceTree();
            inheritanceTree.add(this);
        }
        
        
        Set<String> callables = compileCallableMethods();
        callableMethods = new ArrayList<>(callables);
        Collections.sort(callableMethods);
    }
}
