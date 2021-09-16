package com.dc.red5slim.server.scope;

import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.scope.Scope;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/25
 */
public class LiveScope extends Scope {
    public LiveScope(IScope parent, String name, boolean persistent) {
        super(parent, ScopeType.SHARED_OBJECT, name, persistent);
    }
    
    @Override
    public void init() {
        super.init();
        getServer().addMapping("", name, parent.getName());
    }
    
    
}
